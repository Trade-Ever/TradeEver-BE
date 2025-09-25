package com.trever.backend.api.recent.service;

import com.trever.backend.api.favorite.repository.FavoriteRepository;
import com.trever.backend.api.recent.entity.RecentView;
import com.trever.backend.api.recent.repository.RecentViewRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.api.vehicle.service.VehicleOptionService;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentViewService {
    private final RecentViewRepository recentViewRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleOptionService vehicleOptionService;
    private final FavoriteRepository favoriteRepository;


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

    /**
     * 사용자의 최근 본 차량 목록 조회
     * VehicleService의 buildVehicleSummary 로직을 활용하여 일관된 결과 반환
     */
    public VehicleListResponse getRecentViews(Long userId) {
        // 최근 본 차량 목록 조회 (최대 20개까지)
        List<Vehicle> recentVehicles = recentViewRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(RecentView::getVehicle)
                .filter(vehicle -> vehicle.getVehicleStatus() == VehicleStatus.ACTIVE ||
                        vehicle.getVehicleStatus() == VehicleStatus.AUCTIONS)
                .limit(20)
                .collect(Collectors.toList());

        // 찜한 차량 ID 목록 조회
        Set<Long> favoriteVehicleIds = favoriteRepository.findByUserId(userId).stream()
                .map(favorite -> favorite.getVehicle().getId())
                .collect(Collectors.toSet());

        // 차량 정보를 VehicleSummary로 변환
        List<VehicleListResponse.VehicleSummary> summaries = recentVehicles.stream()
                .map(vehicle -> {
                    // 차량 옵션 조회
                    List<String> options = vehicleOptionService.getVehicleOptionNames(vehicle);

                    // 메인 옵션 (최대 3개까지)
                    List<String> mainOptions = options.size() > 3 ? options.subList(0, 3) : options;

                    // 차량 타입 처리
                    String vehicleTypeName = (vehicle.getVehicleType() != null)
                            ? vehicle.getVehicleType().getDisplayName() : "미정";

                    // VehicleSummary 객체 생성
                    VehicleListResponse.VehicleSummary summary = VehicleListResponse.VehicleSummary.builder()
                            .id(vehicle.getId())
                            .vehicleTypeName(vehicleTypeName)
                            .mainOptions(mainOptions)
                            .carNumber(vehicle.getCarNumber())
                            .carName(vehicle.getCarName())
                            .manufacturer(vehicle.getManufacturer())
                            .model(vehicle.getModel())
                            .year_value(vehicle.getYear_value())
                            .mileage(vehicle.getMileage())
                            .transmission(vehicle.getTransmission())
                            .fuelType(vehicle.getFuelType())
                            .price(vehicle.getPrice())
                            .vehicleStatus(vehicle.getVehicleStatus().getDisplayName())
                            .isAuction(vehicle.getIsAuction())
                            .auctionId(vehicle.getAuctionId())
                            .representativePhotoUrl(vehicle.getRepresentativePhotoUrl())
                            .favoriteCount(vehicle.getFavoriteCount())
                            .createdAt(vehicle.getCreatedAt())
                            .totalOptionsCount(options.size())
                            .isFavorite(favoriteVehicleIds.contains(vehicle.getId()))
                            .build();

                    return summary;
                })
                .collect(Collectors.toList());

        // VehicleListResponse 형식으로 결과 반환
        return VehicleListResponse.builder()
                .vehicles(summaries)
                .totalCount(summaries.size())
                .pageNumber(0)
                .pageSize(summaries.size())
                .build();
    }

}
