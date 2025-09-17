package com.trever.backend.auction.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import com.trever.backend.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "auctions")
public class Auction extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Float startPrice;

    private LocalDateTime startAt;
    
    private LocalDateTime endAt;
    
    @Enumerated(EnumType.STRING)
    private AuctionStatus status;
    
    @OneToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    // 현재 입찰가를 위한 필드 추가 (Firebase와 동기화를 위한 필드)
    @Transient
    private Float currentBidPrice;
    
    @Transient
    private Long currentBidUserId;
    
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == AuctionStatus.ACTIVE && 
               startAt.isBefore(now) && 
               endAt.isAfter(now);
    }
    
    public boolean canBid(Float bidPrice) {
        if (!isActive()) {
            return false;
        }
        
        if (currentBidPrice == null) {
            return bidPrice >= startPrice;
        }
        
        return bidPrice > currentBidPrice;
    }
}
