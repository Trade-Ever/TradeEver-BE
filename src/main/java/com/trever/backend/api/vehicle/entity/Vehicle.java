package com.trever.backend.api.vehicle.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import com.trever.backend.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "vehicles")
public class Vehicle extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String carNumber;

    private String description;
    
    private String manufacturer;

    private String carName;

    private String model;
    
    private Integer year_value;
    
    private Integer mileage;
    
    private String fuelType;
    
    private String transmission;
    
    private Character accidentHistory;

    private String accidentDescription;
    
    private String vehicleStatus;
    
    private Integer engineCc;
    
    private Integer horsepower;
    
    private String color;

    private String additionalInfo;
    
    private Long price;
    
    private Character isAuction;
    
    private Long auctionId;

    private String locationAddress;
    
    private Integer favoriteCount;

    // 차종 필드 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    // 옵션과의 매핑 관계 추가
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VehicleOptionMapping> optionMappings = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VehiclePhoto> photos = new ArrayList<>();

    // 옵션 추가 편의 메서드
    public void addOption(VehicleOption option) {
        VehicleOptionMapping mapping = VehicleOptionMapping.builder()
                .vehicle(this)
                .option(option)
                .build();
        this.optionMappings.add(mapping);
    }

    @Column(name = "representative_photo_url", length = 1000) // 대표 사진 URL 필드 추가
    private String representativePhotoUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
}
