package com.trever.backend.auction.service;

import com.trever.backend.auction.dto.*;
import com.trever.backend.auction.entity.Auction;
import com.trever.backend.auction.entity.AuctionStatus;
import com.trever.backend.auction.entity.Bid;
import com.trever.backend.auction.repository.AuctionRepository;
import com.trever.backend.auction.repository.BidRepository;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.user.entity.User;
import com.trever.backend.user.service.UserWalletService;
import com.trever.backend.vehicle.entity.Vehicle;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final FirebaseRealtimeService firebaseRealtimeService;
    private final UserWalletService userWalletService; // 추가된 필드
    
    // 가정: Vehicle과 User 정보를 가져오기 위한 Repository들
    // private final VehicleRepository vehicleRepository;
    // private final UserRepository userRepository;
    
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
    public synchronized BidResponse placeBid(BidRequest request, User bidder) {
        // 동시 입찰 처리를 위해 synchronized 키워드 추가
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new NotFoundException("해당 경매를 찾을 수 없습니다: " + request.getAuctionId()));

        //판매자 입찰방지
        if(auction.getVehicle().getSeller().getId().equals(bidder.getId())) {
            throw new BadRequestException("판매자는 입찰이 불가합니다");
        }

        // 경매 활성 상태 검증
        if (!auction.isActive()) {
            throw new BadRequestException("현재 경매가 활성 상태가 아닙니다.");
        }
        
        // 최고 입찰가 조회
        Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(request.getAuctionId());
        Float currentHighestBid = highestBid.map(Bid::getBidPrice).orElse(auction.getStartPrice());
        
        // 입찰가 검증
        if (request.getBidPrice() <= currentHighestBid) {
            throw new BadRequestException("입찰가는 현재 최고 입찰가보다 높아야 합니다.");
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
                .build();
        
        // 이전 최고 입찰자의 자금을 다시 지갑에 반환 (필요한 경우)
        highestBid.ifPresent(previousBid -> {
            userWalletService.deposit(previousBid.getBidder().getId(), previousBid.getBidPrice());
        });
        
        bid = bidRepository.save(bid);
        
        // Firebase에 입찰 정보 업데이트
        firebaseRealtimeService.addBid(bid, bidder.getName());
        
        // 현재 입찰자의 자금을 일시적으로 보류
        userWalletService.withdraw(bidder.getId(), request.getBidPrice());
        
        return BidResponse.builder()
                .id(bid.getId())
                .bidPrice(bid.getBidPrice())
                .bidderId(bid.getBidder().getId())
                .bidderName(bid.getBidder().getName())
                .createdAt(bid.getCreatedAt())
                .auctionId(bid.getAuction().getId())
                .isHighestBid(true)
                .build();
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
     * 경매 종료 처리 (스케줄러)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void endExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findByEndAtBefore(now);
        
        for (Auction auction : expiredAuctions) {
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                // 최고 입찰자 조회
                Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(auction.getId());
                
                // 최고 입찰자가 있는 경우 거래 처리, 없으면 유찰 처리
                if (highestBid.isPresent()) {
                    auction.setStatus(AuctionStatus.ENDED);
                    Bid winningBid = highestBid.get();
                    User buyer = winningBid.getBidder();
                    User seller = auction.getVehicle().getSeller();
                    
                    // TODO: 여기에서 Transaction 엔티티를 생성하고 저장하는 로직 추가
                    // createTransaction(auction, winningBid, buyer, seller);
                    
                    // TODO: 판매자 지갑에 낙찰 금액 입금 로직 추가 (수수료 차감 후)
                    // userWalletService.deposit(seller.getId(), calculateSellerProfit(winningBid.getBidPrice()));
                    
                    log.info("경매가 종료되었습니다. 경매 ID: {}, 낙찰자: {}, 낙찰가: {}", 
                            auction.getId(), buyer.getName(), winningBid.getBidPrice());
                    
                    // Firebase에 상태 업데이트 - 낙찰 완료
                    firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.ENDED.name());
                } else {
                    // 입찰자가 없는 경우 유찰(EXPIRED) 처리
                    auction.setStatus(AuctionStatus.EXPIRED);
                    log.info("경매가 입찰 없이 유찰되었습니다. 경매 ID: {}", auction.getId());
                    
                    // Firebase에 상태 업데이트 - 유찰
                    firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.EXPIRED.name());
                }
                
                auctionRepository.save(auction);
            }
        }
    }
    
    /**
     * 경매 시작 처리 (스케줄러)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void startScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctionsToStart = auctionRepository.findByStatus(AuctionStatus.UPCOMING);
        
        for (Auction auction : auctionsToStart) {
            if (auction.getStartAt().isBefore(now)) {
                auction.setStatus(AuctionStatus.ACTIVE);
                auctionRepository.save(auction);
                
                // Firebase에 상태 업데이트
                firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.ACTIVE.name());
                
                log.info("경매가 시작되었습니다. 경매 ID: {}", auction.getId());
            }
        }
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
        Float currentBidPrice = highestBid.map(Bid::getBidPrice).orElse(null);
        
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
