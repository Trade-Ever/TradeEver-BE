package com.trever.backend.api.trade.entity;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL)
    private Contract contract;
}
