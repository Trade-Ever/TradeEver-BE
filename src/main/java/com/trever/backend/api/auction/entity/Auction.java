package com.trever.backend.api.auction.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vehicleId; // 차량 ID
    private Long startPrice; // 시작가
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status; // 진행중, 종료 등
}