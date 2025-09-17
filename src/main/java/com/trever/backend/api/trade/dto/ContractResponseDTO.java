package com.trever.backend.api.trade.dto;

import com.trever.backend.api.trade.entity.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContractResponseDTO {

    private Long id;
    private Long transactionId;

    // 계약 상태 정보
    private boolean signedByBuyer;
    private boolean signedBySeller;
    private LocalDateTime signedAt;

    // 최종 계약서 pdf (완료 전이면 null)
    private String contractPdfUrl;

    // 정적 팩토리 메서드
    public static ContractResponseDTO from(Contract contract) {
        return ContractResponseDTO.builder()
                .id(contract.getId())
                .transactionId(contract.getTransaction().getId())
                .signedByBuyer(contract.isSignedByBuyer())
                .signedBySeller(contract.isSignedBySeller())
                .signedAt(contract.getSignedAt())
                .contractPdfUrl(contract.getContractPdfUrl()) // 완료 전이면 null
                .build();
    }
}
