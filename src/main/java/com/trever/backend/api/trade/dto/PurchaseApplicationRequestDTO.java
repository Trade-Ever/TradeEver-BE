package com.trever.backend.api.trade.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseApplicationRequestDTO {
    private Long buyerId;
    private Long vehicleId;
}