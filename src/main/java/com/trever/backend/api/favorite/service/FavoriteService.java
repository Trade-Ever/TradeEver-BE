package com.trever.backend.api.favorite.service;

import com.trever.backend.api.favorite.entity.Favorite;
import com.trever.backend.api.favorite.repository.FavoriteRepository;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final VehicleRepository vehicleRepository;

    // 찜 목록 조회
    public List<VehicleListResponse.VehicleSummary> getFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(fav -> {
                    Vehicle v = vehicleRepository.findById(fav.getVehicleId())
                            .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

                    return VehicleListResponse.VehicleSummary.builder()
                            .id(v.getId())
                            .carNumber(v.getCarNumber())
                            .carName(v.getCarName())
                            .manufacturer(v.getManufacturer())
                            .model(v.getModel())
                            .year_value(v.getYear_value())
                            .mileage(v.getMileage())
                            .transmission(v.getTransmission())
                            .fuelType(v.getFuelType())
                            .price(v.getPrice())
                            .isAuction(v.getIsAuction())
                            .auctionId(v.getAuctionId())
                            .representativePhotoUrl(v.getRepresentativePhotoUrl())
                            .locationAddress(v.getLocationAddress())
                            .favoriteCount(v.getFavoriteCount())
                            .createdAt(v.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // 찜 추가
    public void addFavorite(Long userId, Long vehicleId) {
        // Vehicle 존재 여부 검증
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        // 중복 찜 방지
        if (favoriteRepository.existsByUserIdAndVehicleId(userId, vehicleId)) {
            throw new NotFoundException(ErrorStatus.FAVORITE_ALREADY_EXISTS.getMessage());
        }

        Favorite favorite = Favorite.builder()
                .userId(userId)
                .vehicleId(vehicle.getId())
                .build();

        favoriteRepository.save(favorite);
    }

    // 찜 삭제
    public void removeFavorite(Long userId, Long vehicleId) {
        favoriteRepository.deleteByUserIdAndVehicleId(userId, vehicleId);
    }
}
