package com.trever.backend.api.vehicle.dto;

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
public class VehicleListResponse {
    private List<VehicleSummary> vehicles;
    private int totalCount;
    private int pageNumber;
    private int pageSize;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleSummary {
        private Long id;
        private String carName;
        private String carNumber;
        private String manufacturer;
        private String model;
        private Integer year_value;
        private Integer mileage;
        private String transmission;
        private String fuelType;
        private Long price;
        private Character isAuction;
        private Long auctionId;
        private String representativePhotoUrl; 
        private String locationAddress;
        private Integer favoriteCount;
        private LocalDateTime createdAt;
    }
}