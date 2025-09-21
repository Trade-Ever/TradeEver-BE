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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionBidQueueService {
    private final Map<Long, Queue<BidTask>> bidQueues = new ConcurrentHashMap<>();
    private final Map<Long, Thread> processingThreads = new ConcurrentHashMap<>();
    
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserWalletService userWalletService;
    private final FirebaseRealtimeService firebaseRealtimeService;
    private final AuctionLockService auctionLockService;
    
    @Data
    @AllArgsConstructor
    public static class BidTask {
        private BidRequest request;
        private User bidder;
        private CompletableFuture<BidResponse> future;
    }
    
    /**
     * 입찰 요청을 큐에 추가하고 비동기로 처리
     */
    public CompletableFuture<BidResponse> queueBid(BidRequest request, User bidder) {
        CompletableFuture<BidResponse> future = new CompletableFuture<>();
        
        Long auctionId = request.getAuctionId();
        bidQueues.computeIfAbsent(auctionId, k -> new ConcurrentLinkedQueue<>())
                .add(new BidTask(request, bidder, future));
        
        log.debug("입찰 큐에 추가됨 - 경매 ID: {}, 입찰자: {}, 입찰가: {}", 
                auctionId, bidder.getName(), request.getBidPrice());
        
        // 큐 처리 스레드가 없으면 시작
        if (!processingThreads.containsKey(auctionId)) {
            Thread processingThread = new Thread(() -> processBidQueue(auctionId));
            processingThread.setDaemon(true);
            processingThread.start();
            processingThreads.put(auctionId, processingThread);
            log.debug("입찰 처리 스레드 시작 - 경매 ID: {}", auctionId);
        }
        
        return future;
    }
    
    /**
     * 특정 경매의 입찰 큐를 처리하는 메서드
     */
    private void processBidQueue(Long auctionId) {
        try {
            while (true) {
                Queue<BidTask> queue = bidQueues.get(auctionId);
                BidTask task = queue.poll();
                
                if (task == null) {
                    // 큐가 비었으면 스레드 종료
                    processingThreads.remove(auctionId);
                    log.debug("입찰 처리 스레드 종료 - 경매 ID: {}", auctionId);
                    break;
                }
                
                try {
                    log.debug("입찰 처리 시작 - 경매 ID: {}, 입찰자: {}, 입찰가: {}", 
                            auctionId, task.getBidder().getName(), task.getRequest().getBidPrice());
                    
                    // 기존 AuctionService.processPlaceBid() 메서드의 내용을 여기로 이동
                    BidResponse response = processPlaceBid(task.getRequest(), task.getBidder());
                    
                    task.getFuture().complete(response);
                    log.debug("입찰 처리 완료 - 경매 ID: {}, 입찰 ID: {}", auctionId, response.getId());
                } catch (Exception e) {
                    log.error("입찰 처리 중 오류 - 경매 ID: {}, 오류: {}", auctionId, e.getMessage(), e);
                    task.getFuture().completeExceptionally(e);
                }
                
                // 다음 작업 전에 잠시 대기
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("입찰 처리 스레드 중단 - 경매 ID: {}", auctionId);
        }
    }
    
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
     * 모든 큐의 현재 상태를 조회
     */
    public Map<Long, Integer> getQueueStatus() {
        Map<Long, Integer> status = new ConcurrentHashMap<>();
        bidQueues.forEach((auctionId, queue) -> status.put(auctionId, queue.size()));
        return status;
    }
}