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
