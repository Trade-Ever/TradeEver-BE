package com.trever.backend.api.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sellerId; // 판매자 ID (users 테이블 FK)

    @Column(nullable = false)
    private String title; // 매물 제목

    private String manufacturer; // 제조사
    private String model;        // 차량 모델

    @Column(name = "car_year") // year → car_year 로 매핑
    private Integer year;

    private Integer mileage;     // 주행거리 (km)

    @Column(nullable = false)
    private Long price; // 판매가

    @Column(nullable = false)
    private String vehicleStatus = "판매중"; // 판매중, 거래완료 등

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
