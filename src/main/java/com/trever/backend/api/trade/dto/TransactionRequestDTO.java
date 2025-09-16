package com.trever.backend.api.trade.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequestDTO {
    private Long buyerId;
    private Long vehicleId;
}
