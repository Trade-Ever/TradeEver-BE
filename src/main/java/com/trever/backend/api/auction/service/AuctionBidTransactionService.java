package com.trever.backend.api.auction.service;

import com.trever.backend.api.auction.dto.BidRequest;
import com.trever.backend.api.auction.dto.BidResponse;
import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.entity.AuctionStatus;
import com.trever.backend.api.auction.entity.Bid;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.auction.repository.BidRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.service.UserWalletService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionBidTransactionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserWalletService userWalletService;
    private final FirebaseRealtimeService firebaseRealtimeService;
    private final AuctionLockService auctionLockService;

    /**
     * 실제 입찰 처리 로직 (AuctionService에서 이동)
     */
    @Transactional
    public BidResponse processPlaceBid(BidRequest request, User bidder) {
        // 현재 시간 기록
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

            // *** 시간 기반으로만 입찰 가능 여부 판단 ***
            
            // 1. 시작 시간 검증
            if (now.isBefore(auction.getStartAt())) {
                throw new BadRequestException("경매가 아직 시작되지 않았습니다. 시작 시간: " + auction.getStartAt());
            }
            
            // 2. 종료 시간 검증
            if (now.isAfter(auction.getEndAt())) {
                // 경매가 종료되었으나 스케줄러에서 아직 상태 변경이 되지 않았을 수 있음
                // 여기서 상태를 PENDING_CLOSE로 변경해서 스케줄러가 처리할 수 있게 함
                if (auction.getStatus() == AuctionStatus.ACTIVE) {
                    auction.setStatus(AuctionStatus.PENDING_CLOSE);
                    auctionRepository.save(auction);
                    firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.PENDING_CLOSE.name());
                    log.info("경매가 종료 시간이 지나 PENDING_CLOSE로 상태 변경: 경매 ID {}", auction.getId());
                }
                throw new BadRequestException("경매가 이미 종료되었습니다. 종료 시간: " + auction.getEndAt());
            }
            
            // 3. ACTIVE 상태가 아닌 경우에는 로그만 남기고 진행
            // 스케줄러에서 아직 상태를 업데이트하지 않았을 수 있음
            if (auction.getStatus() != AuctionStatus.ACTIVE) {
                log.warn("경매 상태가 ACTIVE가 아니지만 시간 기준으로 입찰 가능 상태입니다. 경매 ID: {}, 현재 상태: {}", 
                        auction.getId(), auction.getStatus());
                
                // 스케줄러가 아직 처리하지 않았고, 시간은 경매 중인 경우 상태를 직접 변경
                if (auction.getStatus() == AuctionStatus.UPCOMING && 
                    now.isAfter(auction.getStartAt()) && now.isBefore(auction.getEndAt())) {
                    auction.setStatus(AuctionStatus.ACTIVE);
                    auctionRepository.save(auction);
                    firebaseRealtimeService.updateAuctionStatus(auction.getId(), AuctionStatus.ACTIVE.name());
                    log.info("스케줄러 처리 전 입찰 요청으로 경매 상태를 ACTIVE로 변경: 경매 ID {}", auction.getId());
                }
            }

            // 최고 입찰가 조회
            Optional<Bid> highestBid = bidRepository.findHighestBidByAuctionId(request.getAuctionId());

            // 입찰가 검증
            if (highestBid.isPresent()) {
                // 기존 입찰이 있는 경우 검증
                if (request.getBidPrice() <= highestBid.get().getBidPrice()) {
                    throw new BadRequestException("입찰가는 현재 최고 입찰가보다 높아야 합니다.");
                }
            } else {
                // 최초 입찰인 경우 검증
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
                    .bidTime(now)
                    .build();

            bid = bidRepository.save(bid);

            // 경매의 현재가 업데이트
            auction.setCurrentBidPrice(request.getBidPrice());
            auctionRepository.save(auction);

            // 이전 최고 입찰자의 자금을 다시 지갑에 반환
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
}
