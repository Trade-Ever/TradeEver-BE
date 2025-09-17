package com.trever.backend.auction.dto;

import com.trever.backend.auction.entity.AuctionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDetailResponse {
    private Long id;
    private Float startPrice;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private AuctionStatus status;
    private LocalDateTime createdAt;
    private Long vehicleId;
    private String vehicleCarNumber;
    private String vehicleManufacturer;
    private String vehicleModel;
    private String representativePhotoUrl; // 대표 이미지 URL 추가
    
    
    // 실시간 입찰 정보
    private Float currentBidPrice;
    private Long currentBidUserId;
    private String currentBidUserName;
    private Integer bidCount;
    private LocalDateTime lastBidTime;
    
    // 남은 시간 계산 (클라이언트에서도 계산할 수 있지만, 서버에서 제공)
    private Long remainingTimeInSeconds;
}
