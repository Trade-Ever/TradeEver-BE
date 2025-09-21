package com.trever.backend.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSearchRequest {
    // 기본 검색어 (제목 검색용)
    private String keyword;

    // 차모델 필터링
    private String manufacturer;    // 제조사
    private String carName;         // 차명
    private String carModel;        // 차모델

    // 연식 필터링
    private Integer yearStart;      // 시작 연도
    private Integer yearEnd;        // 종료 연도

    // 주행거리 필터링
    private Integer mileageStart;   // 최소 주행거리
    private Integer mileageEnd;     // 최대 주행거리

    // 가격 필터링
    private Long priceStart;        // 최소 가격
    private Long priceEnd;          // 최대 가격

    // 차종 필터링
    private String vehicleType;     // 차종

    // 페이지네이션
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;
}