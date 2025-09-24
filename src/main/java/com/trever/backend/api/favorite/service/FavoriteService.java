package com.trever.backend.api.favorite.service;

import com.trever.backend.api.favorite.entity.Favorite;
import com.trever.backend.api.favorite.repository.FavoriteRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import com.trever.backend.api.vehicle.service.VehicleOptionService;
import com.trever.backend.api.vehicle.service.VehicleService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final VehicleOptionService vehicleOptionService;

    // 찜 목록 조회
    public VehicleListResponse getFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 찜한 차량 목록 조회
        List<Vehicle> favoriteVehicles = favoriteRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(Favorite::getVehicle)
                .filter(vehicle -> vehicle.getVehicleStatus() == VehicleStatus.ACTIVE ||
                        vehicle.getVehicleStatus() == VehicleStatus.AUCTIONS)
                .collect(Collectors.toList());

        // VehicleSummary 목록으로 변환
        List<VehicleListResponse.VehicleSummary> summaries = favoriteVehicles.stream()
                .map(vehicle -> {
                    // 차량 옵션 조회
                    List<String> options = vehicleOptionService.getVehicleOptionNames(vehicle);

                    // 메인 옵션 (최대 3개까지)
                    List<String> mainOptions = options.size() > 3 ? options.subList(0, 3) : options;

                    // 차량 타입 처리
                    String vehicleTypeName = (vehicle.getVehicleType() != null)
                            ? vehicle.getVehicleType().getDisplayName() : "미정";

                    // VehicleSummary 객체 생성
                    return VehicleListResponse.VehicleSummary.builder()
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
                            .isFavorite(true) // 찜 목록이므로 항상 true
                            .build();
                })
                .collect(Collectors.toList());

        // 목록 응답 생성
        return VehicleListResponse.builder()
                .vehicles(summaries)
                .totalCount(summaries.size())
                .pageNumber(0)
                .pageSize(summaries.size())
                .build();
    }

//    // 찜 추가
//    public void addFavorite(Long userId, Long vehicleId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//        Vehicle vehicle = vehicleRepository.findById(vehicleId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));
//
//        // 중복 찜 방지
//        if (favoriteRepository.existsByUserAndVehicle(user, vehicle)) {
//            throw new BadRequestException(ErrorStatus.FAVORITE_ALREADY_EXISTS.getMessage());
//        }
//
//        Favorite favorite = Favorite.builder()
//                .user(user)
//                .vehicle(vehicle)
//                .build();
//
//        favoriteRepository.save(favorite);
//    }
//
//    // 찜 삭제
//    public void removeFavorite(Long userId, Long vehicleId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//        Vehicle vehicle = vehicleRepository.findById(vehicleId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));
//
//        favoriteRepository.deleteByUserAndVehicle(user, vehicle);
//    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long vehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.VEHICLE_NOT_FOUND.getMessage()));

        //자신의 차량에 찜 방지
        if(user.getId().equals(vehicle.getSeller().getId())) {
            throw new BadRequestException(ErrorStatus.FAVORITE_FORBIDDEN.getMessage());
        }

        boolean exists = favoriteRepository.existsByUserIdAndVehicleId(userId, vehicleId);

        if (exists) {
            // 이미 찜했으면 삭제
            favoriteRepository.deleteByUserIdAndVehicleId(userId, vehicleId);
            vehicle.decreaseFavoriteCount();
            return false; // 찜 해제
        } else {
            // 찜 추가
            favoriteRepository.save(Favorite.builder().user(user).vehicle(vehicle).build());
            vehicle.increaseFavoriteCount();
            return true; // 찜 등록
        }
    }
}
