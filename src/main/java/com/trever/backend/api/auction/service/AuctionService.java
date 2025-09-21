package com.trever.backend.api.auction.service;

import com.trever.backend.api.auction.dto.*;
import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.AuctionStatus;
import com.trever.backend.api.auction.entity.Bid;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.auction.repository.BidRepository;
import com.trever.backend.api.trade.service.TransactionService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.service.UserWalletService;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final TransactionService transactionService;
    private final UserWalletService userWalletService;
    private final FirebaseRealtimeService firebaseRealtimeService;
    private final AuctionLockService auctionLockService;
    private final AuctionBidQueueService auctionBidQueueService;

    
    /**
     * 새로운 경매 생성
     */
    @Transactional
    public Long createAuction(AuctionCreateRequest request, Vehicle vehicle) {
        validateAuctionRequest(request);
        
        Auction auction = Auction.builder()
                .startPrice(request.getStartPrice())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(determineInitialStatus(request.getStartAt()))
                .vehicle(vehicle)
                .build();

        auction = getAuction(auction);

        // Firebase에 경매 정보 추가
        firebaseRealtimeService.updateAuctionData(auction);
        
        return auction.getId();
    }

    private Auction getAuction(Auction auction) {
        auction = auctionRepository.save(auction);
        return auction;
    }

    /**
     * 경매 상세 정보 조회
     */
    public AuctionDetailResponse getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NotFoundException("해당 경매를 찾을 수 없습니다: " + auctionId));
        
        // 최고 입찰 정보 조회
        Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(auctionId);
        
        // 총 입찰 수 조회
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidPriceDesc(auctionId);
        
        AuctionDetailResponse response = AuctionDetailResponse.builder()
                .id(auction.getId())
                .startPrice(auction.getStartPrice())
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .vehicleId(auction.getVehicle().getId())
                .vehicleCarNumber(auction.getVehicle().getCarNumber())
                .vehicleManufacturer(auction.getVehicle().getManufacturer())
                .vehicleModel(auction.getVehicle().getModel())
                .bidCount(bids.size())
                .representativePhotoUrl(auction.getVehicle().getRepresentativePhotoUrl())
                .build();
        
        // 최고 입찰 정보 설정
        highestBid.ifPresent(bid -> {
            response.setCurrentBidPrice(bid.getBidPrice());
            response.setCurrentBidUserId(bid.getBidder().getId());
            response.setCurrentBidUserName(bid.getBidder().getName());
            response.setLastBidTime(bid.getCreatedAt());
        });
        
        // 남은 시간 계산
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            response.setRemainingTimeInSeconds(
                    calculateRemainingTime(auction.getEndAt())
            );
        }
        
        return response;
    }
    
    /**
     * 경매 목록 조회 (상태별, 페이징)
     */
    public AuctionListResponse getAuctions(AuctionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("endAt").ascending());
        
        Page<Auction> auctions;
        if (status == null) {
            // 상태가 null일 경우 전체 조회
            auctions = auctionRepository.findAll(pageable);
        } else {
            // 상태별 필터링
            if (status == AuctionStatus.ACTIVE) {
                // ACTIVE 상태는 현재 시간이 시작-종료 사이인 것만 조회
                LocalDateTime now = LocalDateTime.now();
                auctions = auctionRepository.findByStatusAndStartAtBeforeAndEndAtAfter(
                        status, now, now, pageable);
            } else {
                // 다른 상태는 단순히 상태로만 필터링
                auctions = auctionRepository.findByStatus(status, pageable);
            }
        }
        
        log.info("조회된 경매 수: {}, 상태: {}", auctions.getTotalElements(), status);
        
        List<AuctionListResponse.AuctionSummary> auctionSummaries = new ArrayList<>();
        for (Auction auction : auctions) {
            AuctionListResponse.AuctionSummary summary = mapToAuctionSummary(auction);
            auctionSummaries.add(summary);
        }
        
        return AuctionListResponse.builder()
                .auctions(auctionSummaries)
                .totalCount((int) auctions.getTotalElements())
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    /**
     * 새로운 입찰 처리
     */
    @Transactional
    public CompletableFuture<BidResponse> placeBid(BidRequest request, User bidder) {
        return auctionBidQueueService.queueBid(request, bidder);
    }


    /**
     * 실제 입찰 처리 (큐 서비스에서 호출)
     */
    @Transactional
    public BidResponse processPlaceBid(BidRequest request, User bidder) {
        // 현재 시간 기록 (모든 시간 비교에 이 값을 사용)
        LocalDateTime now = LocalDateTime.now();
        String requestId = UUID.randomUUID().toString();
        
        // 분산 환경에서의 동시성 제어를 위한 락 획득
        boolean lockAcquired = auctionLockService.acquireLock(request.getAuctionId(), requestId, 10000);
        if (!lockAcquired) {
            throw new BadRequestException("다른 입찰 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
        
        try {
            Auction auction = auctionRepository.findById(request.getAuctionId())
                    .orElseThrow(() -> new NotFoundException("해당 경매를 찾을 수 없습니다: " + request.getAuctionId()));

            // 판매자 입찰 방지
            if (auction.getVehicle().getSeller().getId().equals(bidder.getId())) {
                throw new BadRequestException("판매자는 입찰이 불가합니다");
            }

            // 경매 상태 검증 (ACTIVE 상태인지)
            if (auction.getStatus() != AuctionStatus.ACTIVE) {
                throw new BadRequestException("현재 경매가 활성 상태가 아닙니다. 현재 상태: " + auction.getStatus());
            }
            
            // 명시적 시간 검증 추가 - 시작 시간 확인
            if (now.isBefore(auction.getStartAt())) {
                throw new BadRequestException("경매가 아직 시작되지 않았습니다. 시작 시간: " + auction.getStartAt());
            }
            
            // 명시적 시간 검증 추가 - 종료 시간 확인
            if (now.isAfter(auction.getEndAt()) || now.isEqual(auction.getEndAt())) {
                // 스케줄러가 아직 처리하지 않은 종료된 경매라면 여기서 종료 처리
                auction.setStatus(AuctionStatus.PENDING_CLOSE);
                auctionRepository.save(auction);
                throw new BadRequestException("경매가 이미 종료되었습니다. 종료 시간: " + auction.getEndAt());
            }
            
            // 최고 입찰가 조회
            Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(request.getAuctionId());
            
            // 입찰가 검증 - 최초 입찰인 경우 시작가와 동일해도 허용
            if (highestBid.isPresent()) {
                // 기존 입찰이 있는 경우 - 새 입찰가가 더 높아야 함
                if (request.getBidPrice() <= highestBid.get().getBidPrice()) {
                    throw new BadRequestException("입찰가는 현재 최고 입찰가보다 높아야 합니다.");
                }
            } else {
                // 최초 입찰인 경우 - 시작가 이상이어야 함 (동일해도 허용)
                if (request.getBidPrice() < auction.getStartPrice()) {
                    throw new BadRequestException("입찰가는 시작가 이상이어야 합니다.");
                }
            }
            
            // 본인의 이전 입찰이 최고 입찰인 경우 추가 입찰 방지
            if (highestBid.isPresent() && highestBid.get().getBidder().getId().equals(bidder.getId())) {
                throw new BadRequestException("이미 최고 입찰자입니다. 다른 입찰자가 더 높은 금액을 제시할 때까지 기다려주세요.");
            }
            
            // 입찰자의 잔액 확인
            if (!userWalletService.hasSufficientFunds(bidder.getId(), request.getBidPrice())) {
                throw new BadRequestException("잔액이 부족합니다.");
            }
            
            // 새로운 입찰 생성 및 저장
            Bid bid = Bid.builder()
                    .bidPrice(request.getBidPrice())
                    .bidder(bidder)
                    .auction(auction)
                    .bidTime(now)  // 정확한 입찰 시간 기록
                    .build();
            
            bid = bidRepository.save(bid);
            
            // 경매의 현재가 업데이트
            auction.setCurrentBidPrice(request.getBidPrice());
            auctionRepository.save(auction);
            
            // 이전 최고 입찰자의 자금을 다시 지갑에 반환 (필요한 경우)
            highestBid.ifPresent(previousBid -> {
                userWalletService.deposit(previousBid.getBidder().getId(), previousBid.getBidPrice());
                log.info("이전 입찰자 {}의 입찰금 {}원 반환", previousBid.getBidder().getId(), previousBid.getBidPrice());
            });
            
            // 현재 입찰자의 자금을 일시적으로 보류
            userWalletService.withdraw(bidder.getId(), request.getBidPrice());
            log.info("새 입찰자 {}의 입찰금 {}원 보류", bidder.getId(), request.getBidPrice());
            
            // Firebase에 입찰 정보 업데이트
            firebaseRealtimeService.addBid(bid, bidder.getName());
            firebaseRealtimeService.updateCurrentPrice(auction.getId(), request.getBidPrice());
            
            return BidResponse.builder()
                    .id(bid.getId())
                    .bidPrice(bid.getBidPrice())
                    .bidderId(bidder.getId())
                    .bidderName(bidder.getName())
                    .createdAt(bid.getCreatedAt())
                    .auctionId(auction.getId())
                    .isHighestBid(true)
                    .isWaiting(false)
                    .build();
                
        } finally {
            // 락 해제
            auctionLockService.releaseLock(request.getAuctionId(), requestId);
        }
    }
    
    /**
     * 경매 취소
     */
    @Transactional
    public void cancelAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NotFoundException("해당 경매를 찾을 수 없습니다: " + auctionId));
        
        // 이미 종료된 경매는 취소할 수 없음
        if (auction.getStatus() == AuctionStatus.ENDED) {
            throw new BadRequestException("이미 종료된 경매는 취소할 수 없습니다.");
        }
        
        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
        
        // Firebase에서 상태 업데이트 및 입찰 정보 삭제
        firebaseRealtimeService.updateAuctionStatus(auctionId, AuctionStatus.CANCELLED.name());
    }
    
    /**
     * 경매 종료 처리 (매 분 정각에 실행)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매 분 0초에 실행
    @Transactional
    public void endExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);
        
        log.debug("경매 종료 스케줄러 실행: {}", now);
        
        // 1. 지난 1분 동안 종료되어야 하는 ACTIVE 경매 조회
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndAtBetween(
                AuctionStatus.ACTIVE, 
                oneMinuteAgo.withSecond(0).withNano(0),                // 1분 전의 00초 (포함)
                oneMinuteAgo.withSecond(59).withNano(999_999_999)      // 1분 전의 59.999999999초 (포함)
        );
        
        // 2. PENDING_CLOSE 상태의 경매도 함께 처리
        List<Auction> pendingCloseAuctions = auctionRepository.findByStatus(AuctionStatus.PENDING_CLOSE);
        
        // 두 리스트 합치기
        List<Auction> auctionsToProcess = new ArrayList<>(expiredAuctions);
        auctionsToProcess.addAll(pendingCloseAuctions);
        
        if (!auctionsToProcess.isEmpty()) {
            log.info("종료 처리할 경매: {} 건", auctionsToProcess.size());
        }
        
        for (Auction auction : auctionsToProcess) {
            processAuctionEnd(auction, now);
        }
        
        // 안전장치: 놓친 종료 경매가 있는지 확인 (매 10분마다)
        if (now.getMinute() % 10 == 0) {
            List<Auction> missedAuctions = auctionRepository.findByStatusAndEndAtBefore(
                    AuctionStatus.ACTIVE, 
                    now.minusSeconds(65)  // 현재 시간보다 65초 이전에 종료되었어야 함
            );
            
            if (!missedAuctions.isEmpty()) {
                log.warn("처리되지 않은 종료 경매가 있습니다: {} 건", missedAuctions.size());
                for (Auction auction : missedAuctions) {
                    processAuctionEnd(auction, now);
                }
            }
        }
    }
    
    /**
     * 경매 종료 처리 로직
     */
    private void processAuctionEnd(Auction auction, LocalDateTime now) {
        // 최고 입찰자 조회
        Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(auction.getId());
        
        // 최고 입찰자가 있는 경우 거래 처리, 없으면 유찰 처리
        if (highestBid.isPresent()) {
            auction.setStatus(AuctionStatus.ENDED);
            Bid winningBid = highestBid.get();
            User buyer = winningBid.getBidder();
            User seller = auction.getVehicle().getSeller();
            
            // Transaction 엔티티를 생성하고 저장
            transactionService.createTransactionFromAuction(auction.getId());
            
            log.info("경매가 종료되었습니다. 경매 ID: {}, 종료 시간: {}, 처리 시간: {}, 낙찰자: {}, 낙찰가: {}", 
                    auction.getId(), auction.getEndAt(), now, buyer.getName(), winningBid.getBidPrice());
            
            // Firebase에 상태 업데이트 - 낙찰 완료
            firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.ENDED.name());
        } else {
            // 입찰자가 없는 경우 유찰(EXPIRED) 처리
            auction.setStatus(AuctionStatus.EXPIRED);
            log.info("경매가 입찰 없이 유찰되었습니다. 경매 ID: {}, 종료 시간: {}, 처리 시간: {}", 
                    auction.getId(), auction.getEndAt(), now);
            
            // Firebase에 상태 업데이트 - 유찰
            firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.EXPIRED.name());
        }
        
        auctionRepository.save(auction);
    }
    
    /**
     * 경매 시작 처리 (매일 자정에 실행)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00:00에 실행
    @Transactional
    public void startScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("자정 경매 시작 스케줄러 실행: {}", now);
        
        // 오늘 시작되어야 하는 경매 조회
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        List<Auction> auctionsToStart = auctionRepository.findByStatusAndStartAtEquals(
                AuctionStatus.UPCOMING, 
                startOfDay
        );
        
        if (!auctionsToStart.isEmpty()) {
            log.info("오늘 시작 처리할 경매: {} 건", auctionsToStart.size());
            for (Auction auction : auctionsToStart) {
                processAuctionStart(auction, now);
            }
        }
        
        // 안전장치: 놓친 시작 경매가 있는지 확인
        List<Auction> missedAuctions = auctionRepository.findByStatusAndStartAtBefore(
                AuctionStatus.UPCOMING, 
                startOfDay.minusDays(1)
        );
        
        if (!missedAuctions.isEmpty()) {
            log.warn("지연 시작된 경매가 있습니다: {} 건", missedAuctions.size());
            for (Auction auction : missedAuctions) {
                processAuctionStart(auction, now);
            }
        }
    }
    
    /**
     * 경매 시작 처리 로직
     */
    private void processAuctionStart(Auction auction, LocalDateTime now) {
        auction.setStatus(AuctionStatus.ACTIVE);
        auctionRepository.save(auction);
        
        // Firebase에 상태 업데이트
        firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.ACTIVE.name());
        
        log.info("경매가 시작되었습니다. 경매 ID: {}, 시작 시간: {}, 처리 시간: {}", 
                auction.getId(), auction.getStartAt(), now);
    }
    
    // 유틸리티 메서드
    
    private AuctionStatus determineInitialStatus(LocalDateTime startAt) {
        return startAt.isBefore(LocalDateTime.now()) ? AuctionStatus.ACTIVE : AuctionStatus.UPCOMING;
    }
    
    private void validateAuctionRequest(AuctionCreateRequest request) {
        if (request.getStartAt().isAfter(request.getEndAt())) {
            throw new BadRequestException("경매 종료 시간은 시작 시간보다 나중이어야 합니다.");
        }
    }
    
    private Long calculateRemainingTime(LocalDateTime endAt) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endAt)) {
            return 0L;
        }
        return Duration.between(now, endAt).getSeconds();
    }
    
    private AuctionListResponse.AuctionSummary mapToAuctionSummary(Auction auction) {
        // 최고 입찰가 조회
        Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(auction.getId());
        Long currentBidPrice = highestBid.map(Bid::getBidPrice).orElse(null);
        
        // 입찰 수 조회
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidPriceDesc(auction.getId());
        
        // 남은 시간 계산
        Long remainingTime = null;
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            remainingTime = calculateRemainingTime(auction.getEndAt());
        }
        
        // 차량 이미지는 첫번째 이미지를 사용한다고 가정
        String vehicleImageUrl = ""; // 실제로는 VehiclePhotosRepository 등에서 조회
        
        return AuctionListResponse.AuctionSummary.builder()
                .id(auction.getId())
                .vehicleTitle(auction.getVehicle().getModel())
                .representativePhotoUrl(auction.getVehicle().getRepresentativePhotoUrl())
                .startPrice(auction.getStartPrice())
                .currentBidPrice(currentBidPrice)
                .bidCount(bids.size())
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .remainingTimeInSeconds(remainingTime)
                .status(auction.getStatus().name())
                .build();
    }
}
