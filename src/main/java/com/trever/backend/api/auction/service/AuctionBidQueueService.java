package com.trever.backend.api.auction.service;

import com.trever.backend.api.auction.dto.BidRequest;
import com.trever.backend.api.auction.dto.BidResponse;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.common.exception.BadRequestException;
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
            Queue<BidTask> queue = bidQueues.get(auctionId);
            
            while (true) {
                // 1. 큐에서 작업 꺼내기
                BidTask task = queue.poll();
                
                // 2. 작업이 없으면 스레드 종료
                if (task == null) {
                    processingThreads.remove(auctionId);
                    log.debug("입찰 처리 스레드 종료 - 경매 ID: {}", auctionId);
                    break;
                }
                
                try {
                    log.debug("입찰 처리 시작 - 경매 ID: {}, 입찰자: {}, 입찰가: {}", 
                            auctionId, task.getBidder().getName(), task.getRequest().getBidPrice());
                    
                    // 3. 입찰 처리 로직 호출
                    BidResponse response = auctionBidTransactionService.processPlaceBid(task.getRequest(), task.getBidder());
                    
                    // 4. 성공 시 결과 반환
                    task.getFuture().complete(response);
                    log.debug("입찰 처리 완료 - 경매 ID: {}, 입찰 ID: {}", auctionId, response.getId());
                    
                } catch (Exception e) {
                    // 5. 오류 발생 시 예외 처리 및 클라이언트에게 반환
                    log.error("입찰 처리 중 오류 - 경매 ID: {}, 오류: {}", auctionId, e.getMessage());
                    task.getFuture().completeExceptionally(e);
                    
                    // 6. 특정 예외의 경우 추가 조치
                    if (e instanceof BadRequestException) {
                        if (e.getMessage().contains("경매가 아직 시작되지 않았습니다") ||
                            e.getMessage().contains("경매가 이미 종료되었습니다") ||
                            e.getMessage().contains("경매가 활성 상태가 아닙니다")) {
                            
                            log.warn("경매 상태로 인한 입찰 거부 - 큐 처리를 중단합니다. 경매 ID: {}", auctionId);
                            
                            // 6.1. 큐에 남은 모든 작업에 동일한 예외 전달
                            while (!queue.isEmpty()) {
                                BidTask remainingTask = queue.poll();
                                remainingTask.getFuture().completeExceptionally(e);
                            }
                            
                            // 6.2. 스레드 종료
                            processingThreads.remove(auctionId);
                            break;  // while 루프 종료
                        }
                    }
                    
                    // 7. 다른 예외의 경우 다음 작업으로 진행
                }
                
                // 8. 간격을 두고 다음 작업 처리 (선택적)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            // 9. 전체 처리 과정에서 예외 발생 시 스레드 정리
            log.error("입찰 큐 처리 중 심각한 오류 발생 - 경매 ID: {}, 오류: {}", auctionId, e.getMessage(), e);
            processingThreads.remove(auctionId);
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