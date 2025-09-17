package com.trever.backend.auction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {
    
    @NotNull(message = "경매 ID는 필수입니다")
    private Long auctionId;
    
    @NotNull(message = "입찰 가격은 필수입니다")
    @Positive(message = "입찰 가격은 양수여야 합니다")
    private Float bidPrice;
    
    @NotNull(message = "입찰자 ID는 필수입니다")
    private Long bidderId;
}
