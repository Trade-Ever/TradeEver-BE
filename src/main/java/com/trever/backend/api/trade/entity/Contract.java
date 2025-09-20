package com.trever.backend.api.trade.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 거래와 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    private String contractPdfUrl; // PDF 경로

    @Column(columnDefinition = "TEXT")
    private String contractData; // JSON/Text로 계약 내용 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;


    private LocalDateTime signedAt;

    // 구매자가 서명했을 때 호출하는 메서드
    public void signAsBuyer() {
        if (status == ContractStatus.PENDING) {
            status = ContractStatus.WAITING_FOR_SELLER;
        } else if (status == ContractStatus.WAITING_FOR_BUYER) {
            status = ContractStatus.COMPLETED;
            signedAt = LocalDateTime.now();
        }
    }

    // 판매자가 서명했을 때 호출하는 메서드
    public void signAsSeller() {
        if (status == ContractStatus.PENDING) {
            status = ContractStatus.WAITING_FOR_BUYER;
        } else if (status == ContractStatus.WAITING_FOR_SELLER) {
            status = ContractStatus.COMPLETED;
            signedAt = LocalDateTime.now();
        }
    }
}
