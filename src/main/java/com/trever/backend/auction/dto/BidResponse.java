package com.trever.backend.auction.dto;

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
    private Float bidPrice;
    private Long bidderId;
    private String bidderName;
    private LocalDateTime createdAt;
    private Long auctionId;
    private Boolean isHighestBid;
}
