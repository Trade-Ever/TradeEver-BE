package com.trever.backend.api.auction.service;

import com.trever.backend.api.auction.dto.BidRequest;
import com.trever.backend.api.auction.dto.BidResponse;
import com.trever.backend.api.auction.entity.Auction;
import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionBidQueueService {

    private final AuctionBidTransactionService auctionBidTransactionService;
    private final AuctionRepository auctionRepository;
    
    // 경매 ID별 입찰 큐
    private final Map<Long, Queue<BidTask>> bidQueues = new ConcurrentHashMap<>();
    
    // 경매 ID별 처리 스레드
    private final Map<Long, Thread> processingThreads = new ConcurrentHashMap<>();
    
    // 타임아웃 처리를 위한 스케줄러
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    /**
     * 입찰 요청을 큐에 넣고 비동기로 처리
     */
    public CompletableFuture<BidResponse> queueBid(BidRequest request, User bidder) {
        CompletableFuture<BidResponse> future = new CompletableFuture<>();
        
        try {
            // 큐에 넣기 전에 빠른 사전 검증
            Auction auction = auctionRepository.findById(request.getAuctionId())
                    .orElseThrow(() -> new NotFoundException("해당 경매를 찾을 수 없습니다: " + request.getAuctionId()));
            
            LocalDateTime now = LocalDateTime.now();
            
            // 경매 시작 시간 검증
            if (now.isBefore(auction.getStartAt())) {
                throw new BadRequestException("경매가 아직 시작되지 않았습니다. 시작 시간: " + auction.getStartAt());
            }
            
            // 경매 종료 시간 검증
            if (now.isAfter(auction.getEndAt())) {
                throw new BadRequestException("경매가 이미 종료되었습니다. 종료 시간: " + auction.getEndAt());
            }
            
            Long auctionId = request.getAuctionId();
            bidQueues.computeIfAbsent(auctionId, k -> new ConcurrentLinkedQueue<>())
                    .add(new BidTask(request, bidder, future));
            
            // 해당 경매에 대한 처리 스레드가 없으면 새로 생성
            if (!processingThreads.containsKey(auctionId) || !processingThreads.get(auctionId).isAlive()) {
                Thread thread = new Thread(() -> processBidQueue(auctionId));
                thread.setName("AuctionBid-" + auctionId);
                thread.setDaemon(true);  // 데몬 스레드로 설정하여 애플리케이션 종료 시 자동 종료되게 함
                thread.start();
                processingThreads.put(auctionId, thread);
                log.debug("입찰 처리 스레드 시작 - 경매 ID: {}", auctionId);
            }
            
            // 타임아웃 설정 (10초)
            scheduledExecutor.schedule(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new TimeoutException("입찰 처리 시간이 초과되었습니다."));
                    log.warn("입찰 처리 타임아웃 - 경매 ID: {}, 입찰자: {}", request.getAuctionId(), bidder.getName());
                }
            }, 10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            // 큐에 추가하기 전에 발생한 예외는 즉시 반환
            future.completeExceptionally(e);
            log.error("입찰 큐 추가 중 오류: {}", e.getMessage());
        }
        
        return future;
    }

    /**
     * 입찰 큐를 처리하는 메소드
     */
    private void processBidQueue(Long auctionId) {
        try {
            Queue<BidTask> queue = bidQueues.get(auctionId);
            if (queue == null) {
                log.warn("큐가 존재하지 않음 - 경매 ID: {}", auctionId);
                processingThreads.remove(auctionId);
                return;
            }
            
            // 최대 처리 시간 설정 (30초)
            long startTime = System.currentTimeMillis();
            long maxProcessingTime = 30000; // 30초
            
            while (true) {
                // 처리 시간 초과 체크
                if (System.currentTimeMillis() - startTime > maxProcessingTime) {
                    log.warn("입찰 큐 처리 시간 초과 - 경매 ID: {}", auctionId);
                    break;
                }
                
                // 큐에서 작업 꺼내기
                BidTask task = queue.poll();
                
                // 작업이 없으면 종료
                if (task == null) {
                    log.debug("더 이상 처리할 입찰이 없음 - 경매 ID: {}", auctionId);
                    break;
                }
                
                // 이미 완료된 작업은 건너뛰기
                if (task.getFuture().isDone()) {
                    log.debug("이미 처리된 입찰 건너뛰기 - 경매 ID: {}", auctionId);
                    continue;
                }
                
                try {
                    log.debug("입찰 처리 시작 - 경매 ID: {}, 입찰자: {}, 입찰가: {}", 
                            auctionId, task.getBidder().getName(), task.getRequest().getBidPrice());
                    
                    // 입찰 처리
                    BidResponse response = auctionBidTransactionService.processPlaceBid(task.getRequest(), task.getBidder());
                    
                    // 성공 시 결과 반환
                    if (!task.getFuture().isDone()) {
                        task.getFuture().complete(response);
                    }
                    log.debug("입찰 처리 완료 - 경매 ID: {}, 입찰 ID: {}", auctionId, response.getId());
                    
                } catch (Exception e) {
                    // 오류 발생 시 클라이언트에 전달
                    if (!task.getFuture().isDone()) {
                        task.getFuture().completeExceptionally(e);
                    }
                    log.error("입찰 처리 중 오류 - 경매 ID: {}, 오류: {}", auctionId, e.getMessage());
                    
                    // 시스템적 예외: 큐 전체 처리 중단이 필요한 경우
                    if (isSystemException(e)) {
                        log.warn("시스템적 예외로 인한 큐 처리 중단 - 경매 ID: {}, 예외: {}", auctionId, e.getMessage());
                        
                        // 남은 모든 작업에도 동일한 예외 전달하고 큐 비우기
                        while (!queue.isEmpty()) {
                            BidTask remainingTask = queue.poll();
                            if (remainingTask != null && !remainingTask.getFuture().isDone()) {
                                remainingTask.getFuture().completeExceptionally(e);
                            }
                        }
                        break;  // 큐 처리 중단
                    } 
                    // 개별 입찰 예외: 해당 입찰만 실패 처리하고 다음 작업 진행
                    else if (isIndividualBidException(e)) {
                        log.info("개별 입찰 예외 발생 - 다음 작업으로 진행. 경매 ID: {}, 입찰자: {}, 예외: {}", 
                                auctionId, task.getBidder().getName(), e.getMessage());
                        
                        // 다음 작업으로 계속 진행 (continue 필요 없음)
                    }
                    // 알 수 없는 예외: 안전을 위해 큐 처리 중단
                    else {
                        log.error("알 수 없는 예외 발생 - 큐 처리 중단. 경매 ID: {}, 예외: {}", auctionId, e.getMessage(), e);
                        break;  // 큐 처리 중단
                    }
                }
                
                // 부하 방지를 위한 짧은 대기
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("입찰 큐 처리 중 예상치 못한 오류 - 경매 ID: {}, 오류: {}", auctionId, e.getMessage(), e);
        } finally {
            // 처리 완료 후 스레드 정리
            processingThreads.remove(auctionId);
            log.debug("입찰 처리 스레드 종료 - 경매 ID: {}", auctionId);
        }
    }

    /**
     * 입찰 작업을 캡슐화하는 클래스
     */
    @RequiredArgsConstructor
    private static class BidTask {
        private final BidRequest request;
        private final User bidder;
        private final CompletableFuture<BidResponse> future;
        
        public BidRequest getRequest() {
            return request;
        }
        
        public User getBidder() {
            return bidder;
        }
        
        public CompletableFuture<BidResponse> getFuture() {
            return future;
        }
    }
    
    /**
     * 모든 활성 스레드 상태 확인 및 정리 (스케줄러로 정기적 실행)
     */
    public void checkAndCleanThreads() {
        processingThreads.forEach((auctionId, thread) -> {
            if (!thread.isAlive()) {
                log.warn("비활성 스레드 감지 및 제거 - 경매 ID: {}", auctionId);
                processingThreads.remove(auctionId);
            } else if (thread.getState() == Thread.State.BLOCKED || thread.getState() == Thread.State.WAITING) {
                // 스레드가 블럭/대기 상태인 경우 (잠재적 교착 상태)
                log.warn("잠재적 교착 상태 스레드 감지 - 경매 ID: {}, 상태: {}", auctionId, thread.getState());
                // 필요시 인터럽트 처리
                // thread.interrupt();
            }
        });
    }
    
    /**
     * 애플리케이션 종료 시 정리 작업
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 타임아웃 예외
     */
    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
    }
    
    /**
     * 시스템적 예외인지 확인 (큐 처리 중단이 필요한 예외)
     */
    private boolean isSystemException(Exception e) {
        if (e instanceof BadRequestException) {
            String msg = e.getMessage();
            return msg != null && (
                    msg.contains("경매가 아직 시작되지 않았습니다") ||
                    msg.contains("경매가 이미 종료되었습니다") ||
                    msg.contains("경매가 활성 상태가 아닙니다") ||
                    msg.contains("해당 경매를 찾을 수 없습니다") ||
                    msg.contains("경매가 취소되었습니다")
            );
        }
        return e instanceof NotFoundException && e.getMessage().contains("경매");
    }

    /**
     * 개별 입찰 예외인지 확인 (해당 입찰만 실패하고 다음으로 진행)
     */
    private boolean isIndividualBidException(Exception e) {
        if (e instanceof BadRequestException) {
            String msg = e.getMessage();
            return msg != null && (
                    msg.contains("잔액이 부족합니다") ||
                    msg.contains("최소 입찰 금액보다 낮습니다") ||
                    msg.contains("이전 최고 입찰자는 다시 입찰할 수 없습니다") ||
                    msg.contains("판매자는 입찰이 불가합니다")
            );
        }
        return false;
    }
}