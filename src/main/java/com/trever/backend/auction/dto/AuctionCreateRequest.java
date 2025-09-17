package com.trever.backend.auction.dto;

import com.trever.backend.auction.entity.AuctionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionCreateRequest {
    
    @NotNull(message = "시작 가격은 필수입니다")
    @Positive(message = "시작 가격은 양수여야 합니다")
    private Float startPrice;
    
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startAt;
    
    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endAt;
    
    @NotNull(message = "차량 ID는 필수입니다")
    private Long vehicleId;
}
