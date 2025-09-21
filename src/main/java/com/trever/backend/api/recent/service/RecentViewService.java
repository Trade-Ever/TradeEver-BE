package com.trever.backend.api.recent.service;

import com.trever.backend.api.recent.entity.RecentView;
import com.trever.backend.api.recent.repository.RecentViewRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
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
public class RecentViewService {
    private final RecentViewRepository recentViewRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public void addRecentView(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_VEHICLE.getMessage()));

        // 같은 차량 있으면 삭제
        recentViewRepository.deleteByUserIdAndVehicleId(userId, vehicleId);
        recentViewRepository.flush();

        // 새로 저장
        recentViewRepository.save(
                RecentView.builder()
                        .user(user)
                        .vehicle(vehicle)
                        .build()
        );
    }

    public List<VehicleListResponse.VehicleSummary> getRecentViews(Long userId) {
        return recentViewRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(rv -> {
                    Vehicle v = rv.getVehicle();
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
                            .favoriteCount(v.getFavoriteCount())
                            .createdAt(v.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
