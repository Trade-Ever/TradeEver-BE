package com.trever.backend.api.trade.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "purchase_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId;   // 신청한 차량 ID

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "buyer_id", nullable = false)
//    private User buyer;

    private Long buyerId;     // 신청자 (구매자) ID
}
