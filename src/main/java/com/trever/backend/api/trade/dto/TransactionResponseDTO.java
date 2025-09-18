package com.trever.backend.api.trade.dto;

import com.trever.backend.api.trade.entity.Transaction;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDTO {
    private Long transactionId;
    private Long vehicleId;
    private String vehicleName;
    private String buyerName;
    private String sellerName;
    private Long finalPrice;
    private String status;     // 거래 상태 (예: COMPLETED, IN_PROGRESS 등)
    private String createdAt;

    public static TransactionResponseDTO from(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .transactionId(transaction.getId())
                .vehicleId(transaction.getVehicle().getId())
                .vehicleName(transaction.getVehicle().getCarName())
                .buyerName(transaction.getBuyer().getName())
                .sellerName(transaction.getSeller().getName())
                .finalPrice(transaction.getFinalPrice())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt().toString())
                .build();
    }

}

