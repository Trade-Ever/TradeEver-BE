package com.trever.backend.api.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest {
    
    @NotBlank(message = "차량번호는 필수입니다")
    private String carNumber;

    @NotBlank(message = "차량명은 필수입니다")
    private String carName;
    
    private String description;
    
    @NotBlank(message = "제조사는 필수입니다")
    private String manufacturer;
    
    @NotBlank(message = "모델은 필수입니다")
    private String model;
    
    @NotNull(message = "연식은 필수입니다")
    @Positive(message = "연식은 양수여야 합니다")
    private Integer year_value;
    
    @NotNull(message = "주행거리는 필수입니다")
    @Positive(message = "주행거리는 양수여야 합니다")
    private Integer mileage;
    
    @NotBlank(message = "연료 타입은 필수입니다")
    private String fuelType;
    
    @NotBlank(message = "변속기 타입은 필수입니다")
    private String transmission;
    
    @NotNull(message = "사고 이력 여부는 필수입니다")
    private Boolean accidentHistory; // true면 Y, false면 N으로 변환
    
    private String accidentDescription; // accidentHistory가 false(N)면 null 가능
    
    @NotBlank(message = "차량 상태는 필수입니다")
    private String vehicleStatus;
    
    @Positive(message = "배기량은 양수여야 합니다")
    private Integer engineCc;
    
    @Positive(message = "마력은 양수여야 합니다")
    private Integer horsepower;
    
    @NotBlank(message = "색상은 필수입니다")
    private String color;
    
    private String additionalInfo; // null 가능
    
    @NotNull(message = "경매 여부는 필수입니다")
    private Boolean isAuction; // true면 Y, false면 N으로 변환
    
    // isAuction이 false(N)인 경우에만 필수
    private Long price; // isAuction이 true면 null
    
    @NotBlank(message = "위치 주소는 필수입니다")
    private String locationAddress;

        // 이미지 파일들은 컨트롤러에서 별도로 처리
    @Size(max = 5, message = "차량 사진은 최대 5개까지 등록 가능합니다")
    private List<Integer> photoOrders = new ArrayList<>(); // 이미지 순서 정보
    
    // 경매 관련 정보 (isAuction이 true인 경우에만 필요)
    private Long startPrice;
    private String startAt; // LocalDateTime으로 파싱
    private String endAt;   // LocalDateTime으로 파싱
}