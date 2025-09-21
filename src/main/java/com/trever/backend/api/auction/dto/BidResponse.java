package com.trever.backend.api.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse{
    private Long id;
    private Long bidPrice;
    private Long bidderId;
    private String bidderName;
    private LocalDateTime createdAt;
    private Long auctionId;
    private Boolean isHighestBid;

    // 추가: 대기 상태 필드
    private boolean isWaiting;
    private Long waitUntil; // 밀리초 단위 타임스탬프
    private String message; // "다른 입찰 처리 중입니다. 잠시만 기다려 주세요."
}
