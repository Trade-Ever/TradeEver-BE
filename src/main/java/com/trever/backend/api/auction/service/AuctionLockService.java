package com.trever.backend.api.auction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuctionLockService {
    // 메모리에 락 정보 저장 (단일 서버 환경용)
    private final Map<String, LockInfo> lockMap = new ConcurrentHashMap<>();

    /**
     * 락 정보 클래스
     */
    private static class LockInfo {
        String requestId;
        long expiryTime;

        LockInfo(String requestId, long expiryTime) {
            this.requestId = requestId;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * 경매에 대한 락 획득 시도
     */
    public boolean acquireLock(Long auctionId, String requestId, long timeoutMillis) {
        String lockKey = "auction_lock:" + auctionId;
        long expiryTime = System.currentTimeMillis() + timeoutMillis;

        synchronized (lockMap) {
            // 이미 락이 있는지 확인
            LockInfo existingLock = lockMap.get(lockKey);
            if (existingLock != null) {
                // 락이 만료되었는지 확인
                if (System.currentTimeMillis() > existingLock.expiryTime) {
                    // 만료된 락이면 제거하고 새로운 락 획득
                    log.info("만료된 락 제거: {}, 요청 ID: {}", lockKey, existingLock.requestId);
                    lockMap.remove(lockKey);
                } else {
                    // 유효한 락이 있으면 실패
                    return false;
                }
            }

            // 새 락 설정
            lockMap.put(lockKey, new LockInfo(requestId, expiryTime));
            log.debug("락 획득: {}, 요청 ID: {}, 만료 시간: {}", lockKey, requestId, expiryTime);
            return true;
        }
    }

    /**
     * 경매에 대한 락 해제
     */
    public void releaseLock(Long auctionId, String requestId) {
        String lockKey = "auction_lock:" + auctionId;

        synchronized (lockMap) {
            LockInfo lockInfo = lockMap.get(lockKey);
            if (lockInfo != null && requestId.equals(lockInfo.requestId)) {
                lockMap.remove(lockKey);
                log.debug("락 해제: {}, 요청 ID: {}", lockKey, requestId);
            }
        }
    }

    /**
     * 모든 만료된 락 정리 (스케줄러에서 주기적으로 호출)
     */
    public void cleanExpiredLocks() {
        long now = System.currentTimeMillis();
        synchronized (lockMap) {
            lockMap.entrySet().removeIf(entry -> {
                if (entry.getValue().expiryTime < now) {
                    log.debug("만료된 락 정리: {}", entry.getKey());
                    return true;
                }
                return false;
            });
        }
    }
}