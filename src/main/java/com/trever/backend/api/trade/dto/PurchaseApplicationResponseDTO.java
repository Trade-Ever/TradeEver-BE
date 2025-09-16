package com.trever.backend.api.trade.dto;

import com.trever.backend.api.trade.entity.PurchaseApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PurchaseApplicationResponseDTO {
    private Long id;
    private Long buyerId;
    private Long vehicleId;

    public static PurchaseApplicationResponseDTO from(PurchaseApplication application) {
        return PurchaseApplicationResponseDTO.builder()
                .id(application.getId())
                .buyerId(application.getBuyerId())
                .vehicleId(application.getVehicleId())
//                .buyerName(application.getBuyer().getName()) // User 매핑에서 바로 꺼냄
                .build();
    }
}
