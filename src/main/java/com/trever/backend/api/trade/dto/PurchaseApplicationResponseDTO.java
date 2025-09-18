package com.trever.backend.api.trade.dto;

import com.trever.backend.api.trade.entity.PurchaseApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PurchaseApplicationResponseDTO {
    private Long id;  // 신청 id
    private Long buyerId;
    private Long vehicleId;
    private String buyerName;
    private String vehicleName;
    private String createdAt;

    public static PurchaseApplicationResponseDTO from(PurchaseApplication application) {
        return PurchaseApplicationResponseDTO.builder()
                .id(application.getId())
                .buyerId(application.getBuyer().getId())
                .vehicleId(application.getVehicle().getId())
                .buyerName(application.getBuyer().getName())
                .vehicleName(application.getVehicle().getCarName())
                .createdAt(application.getCreatedAt().toString())
                .build();
    }
}
