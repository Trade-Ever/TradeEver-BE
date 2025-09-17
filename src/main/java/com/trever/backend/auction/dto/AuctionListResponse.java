package com.trever.backend.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionListResponse {
    private List<AuctionSummary> auctions;
    private int totalCount;
    private int pageNumber;
    private int pageSize;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuctionSummary {
        private Long id;
        private String vehicleTitle;
        private Float startPrice;
        private Float currentBidPrice;
        private Integer bidCount;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private Long remainingTimeInSeconds;
        private String status;
        private String representativePhotoUrl; // 대표 이미지 URL 추가
    }
}
