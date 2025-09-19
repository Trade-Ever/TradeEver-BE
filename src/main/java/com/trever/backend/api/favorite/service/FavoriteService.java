package com.trever.backend.api.favorite.service;

import com.trever.backend.api.auction.repository.AuctionRepository;
import com.trever.backend.api.favorite.dto.FavoriteResponseDTO;
import com.trever.backend.api.favorite.entity.Favorite;
import com.trever.backend.api.favorite.repository.FavoriteRepository;
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
    private final AuctionRepository auctionRepository;
    private final VehicleRepository vehicleRepository;

    // 찜 목록 조회
    public List<FavoriteResponseDTO> getFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        return favorites.stream().map(fav -> {
            Vehicle v = vehicleRepository.findById(fav.getVehicleId())
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

            // 기본 Vehicle 정보
            var builder = FavoriteResponseDTO.builder()
                    .favoriteId(fav.getId())
                    .createdAt(fav.getCreatedAt())
                    .vehicleId(v.getId())
                    .carName(v.getCarName())
                    .manufacturer(v.getManufacturer())
                    .yearValue(v.getYear_value())
                    .mileage(v.getMileage())
                    .price(v.getPrice())
                    .representativePhotoUrl(v.getRepresentativePhotoUrl())
                    .isAuction(v.getIsAuction() != null && v.getIsAuction() == 'Y');

            // 경매 차량이면 Auction 종료일 추가
            if (v.getIsAuction() != null && v.getIsAuction() == 'Y') {
                auctionRepository.findByVehicleId(v.getId())
                        .ifPresent(auction -> builder.auctionEndAt(auction.getEndAt()));
            }

            return builder.build();
        }).toList();
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
