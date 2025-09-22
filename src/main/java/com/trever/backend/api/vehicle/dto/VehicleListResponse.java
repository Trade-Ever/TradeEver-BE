package com.trever.backend.api.vehicle.dto;

import com.trever.backend.api.vehicle.entity.VehicleStatus;
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
        private String vehicleStatus;
        private String fuelType;
        private Long price;
        private Character isAuction;
        private Long auctionId;
        private String representativePhotoUrl;
        private Integer favoriteCount;
        private LocalDateTime createdAt;
        private Boolean isFavorite;

        // 차종 정보 추가
        private String vehicleTypeName;

        // 옵션 요약 (주요 옵션 3개 정도만 표시)
        private List<String> mainOptions;
        private Integer totalOptionsCount;

        // favorite 필드 setter 추가
        public void setFavorite(boolean favorite) {
            this.isFavorite = favorite;
        }
    }
}