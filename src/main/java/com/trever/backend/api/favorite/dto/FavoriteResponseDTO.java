package com.trever.backend.api.favorite.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteResponseDTO {

    private Long favoriteId;              // 찜 PK
    private LocalDateTime createdAt;      // 찜한 날짜

    private Long vehicleId;               // 차량 PK
    private String carName;               // 차량명
    private String manufacturer;          // 제조사
    private Integer yearValue;            // 연식
    private Integer mileage;              // 주행거리
    private Long price;                   // 가격
    private String representativePhotoUrl;// 대표 이미지

    private boolean isAuction;            // 경매 여부
    private LocalDateTime auctionEndAt;   // 경매 종료일(경매인 경우만)
}
