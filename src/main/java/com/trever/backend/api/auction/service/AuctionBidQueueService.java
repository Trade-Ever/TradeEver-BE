package com.trever.backend.api.auction.service;

import com.trever.backend.api.auction.dto.BidRequest;
import com.trever.backend.api.auction.dto.BidResponse;
import com.trever.backend.api.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionBidQueueService {
    private final Map<Long, Queue<BidTask>> bidQueues = new ConcurrentHashMap<>();
    private final Map<Long, Thread> processingThreads = new ConcurrentHashMap<>();

    private final AuctionBidTransactionService auctionBidTransactionService;
    
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
                    BidResponse response = auctionBidTransactionService.processPlaceBid(task.getRequest(), task.getBidder());
                    
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
     * 모든 큐의 현재 상태를 조회
     */
    public Map<Long, Integer> getQueueStatus() {
        Map<Long, Integer> status = new ConcurrentHashMap<>();
        bidQueues.forEach((auctionId, queue) -> status.put(auctionId, queue.size()));
        return status;
    }
}