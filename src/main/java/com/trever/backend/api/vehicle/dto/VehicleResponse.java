package com.trever.backend.api.vehicle.dto;

import com.trever.backend.api.vehicle.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private Long id;
    private String carNumber;
    private String carName;
    private String description;
    private String manufacturer;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long sellerId;
    private String sellerName;
    private List<VehiclePhotoDto> photos; // 차량 사진 목록 추가

    // 차종 정보 추가
    private String vehicleTypeName;

    // 옵션 목록 추가
    private List<String> options;
}