package com.trever.backend.api.vehicle.service;

import com.trever.backend.api.auction.dto.AuctionCreateRequest;
import com.trever.backend.api.auction.service.AuctionService;
import com.trever.backend.api.favorite.repository.FavoriteRepository;
import com.trever.backend.api.recent.service.RecentViewService;
import com.trever.backend.api.vehicle.dto.*;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.basiccar.service.CarModelService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuctionService auctionService;
    private final VehiclePhotoService vehiclePhotoService;
    private final RecentViewService recentViewService;
    private final VehicleOptionService vehicleOptionService;
    private final CarModelService carModelService;
    private final FavoriteRepository favoriteRepository;

    /**
     * 새 차량 등록
     */
    @Transactional
    public Long createVehicle(VehicleCreateRequest request, List<MultipartFile> photos, Long sellerId) {
        // 사용자 조회
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("판매자를 찾을 수 없습니다: " + sellerId));
        // 경매 여부에 따른 유효성 검증
        if (Boolean.TRUE.equals(request.getIsAuction())) {
            if (request.getPrice() != null) {
                throw new BadRequestException("경매로 등록할 경우 가격을 설정할 수 없습니다.");
            }
            if (request.getStartPrice() == null || request.getStartAt() == null || request.getEndAt() == null) {
                throw new BadRequestException("경매로 등록할 경우 시작 가격, 시작 시간, 종료 시간은 필수입니다.");
            }
        } else {
            if (request.getPrice() == null) {
                throw new BadRequestException("판매로 등록할 경우 가격은 필수입니다.");
            }
        }

        // isAuction에 따라 vehicleStatus 자동 설정
        VehicleStatus status;
        if (request.getVehicleStatus() != null) {
            // 요청에 상태가 있으면 그대로 사용
            status = request.getVehicleStatus();
        } else {
            // 요청에 상태가 없으면 isAuction에 따라 자동 설정
            status = Boolean.TRUE.equals(request.getIsAuction()) ?
                    VehicleStatus.AUCTIONS : VehicleStatus.ACTIVE;
        }
        
        // 차량 엔티티 생성
        Vehicle vehicle = Vehicle.builder()
                .carName(request.getCarName())
                .carNumber(request.getCarNumber())
                .description(request.getDescription())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .year_value(request.getYear_value())
                .mileage(request.getMileage())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .accidentHistory(Boolean.TRUE.equals(request.getAccidentHistory()) ? 'Y' : 'N')
                .accidentDescription(Boolean.TRUE.equals(request.getAccidentHistory()) ? request.getAccidentDescription() : null)
                .vehicleStatus(status)
                .engineCc(request.getEngineCc())
                .horsepower(request.getHorsepower())
                .color(request.getColor())
                .price(Boolean.FALSE.equals(request.getIsAuction()) ? request.getPrice() : null)
                .isAuction(Boolean.TRUE.equals(request.getIsAuction()) ? 'Y' : 'N')
                .favoriteCount(0)
                .seller(seller)
                .vehicleType(request.getVehicleType())
                .build();
                
        // 차량 엔티티 저장
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        
        // 사진 업로드 및 저장
        if (photos != null && !photos.isEmpty()) {
            vehiclePhotoService.uploadAndSaveVehiclePhotos(vehicle, photos, request.getPhotoOrders());
        }

        // 옵션 설정
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            vehicleOptionService.setVehicleOptions(savedVehicle, request.getOptions());
        }
        
        // 경매 등록이 필요한 경우
        if (Boolean.TRUE.equals(request.getIsAuction())) {
            LocalDateTime startAt = parseDateTime(request.getStartAt(),true); // 시작일은 00:00:00
            LocalDateTime endAt = parseDateTime(request.getEndAt(), false);   // 종료일은 23:59:59


            AuctionCreateRequest auctionRequest = AuctionCreateRequest.builder()
                    .startPrice(request.getStartPrice())
                    .startAt(startAt)
                    .endAt(endAt)
                    .vehicleId(vehicle.getId())
                    .build();
            
            Long auctionId = auctionService.createAuction(auctionRequest, vehicle);
            vehicle.setAuctionId(auctionId);
            vehicleRepository.save(vehicle);
        }
        
        return savedVehicle.getId();
    }
    
    /**
     * 차량 상세 조회
     */
    public VehicleResponse getVehicleDetail(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("차량을 찾을 수 없습니다."));

        if (userId != null) {
            recentViewService.addRecentView(userId, vehicleId);
        }

        User user = userRepository.findById(vehicle.getSeller().getId())
                .orElseThrow(() -> new NotFoundException("판매자를 찾을 수 없습니다."));

        List<VehiclePhotoDto> photos = vehiclePhotoService.getVehiclePhotos(vehicle.getId()).stream()
                .map(photo -> VehiclePhotoDto.builder()
                        .id(photo.getId())
                        .photoUrl(photo.getPhotoUrl())
                        .orderIndex(photo.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        // 차량 옵션 조회
        List<String> options = vehicleOptionService.getVehicleOptionNames(vehicle);

        // 경매 여부
        Character isAuction = (vehicle.getVehicleType() != null) ? vehicle.getIsAuction() : 'N';

        // 차량 타입이 null일 경우 처리
        String vehicleTypeName = (vehicle.getVehicleType() != null) ? vehicle.getVehicleType().getDisplayName() : "미정";

        boolean isFavorited = false;
        if (userId != null) {
            isFavorited = favoriteRepository.existsByUserIdAndVehicleId(userId, vehicle.getId());
        }

        // VehicleResponse에 대표 사진 URL 포함
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .manufacturer(vehicle.getManufacturer())
                .carName(vehicle.getCarName())
                .model(vehicle.getModel())
                .year_value(vehicle.getYear_value())
                .mileage(vehicle.getMileage())
                .fuelType(vehicle.getFuelType())
                .transmission(vehicle.getTransmission())
                .accidentHistory(vehicle.getAccidentHistory())
                .accidentDescription(vehicle.getAccidentDescription())
                .description(vehicle.getDescription())
                .favoriteCount(vehicle.getFavoriteCount())
                .isFavorited(isFavorited)
                .vehicleStatus(vehicle.getVehicleStatus().getDisplayName())
                .engineCc(vehicle.getEngineCc())
                .horsepower(vehicle.getHorsepower())
                .color(vehicle.getColor())
                .isAuction(isAuction)
                .price(vehicle.getPrice())
                .vehicleTypeName(vehicleTypeName)
                .options(options) // 옵션 목록 추가
                .photos(photos)
                .sellerId(user.getId())
                .sellerPhone(user.getPhone())
                .sellerName(user.getName())
                .updatedAt(vehicle.getUpdatedAt())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
    
    /**
     * 차량 목록 조회
     */
    public VehicleListResponse getVehicles(int page, int size, String sortBy, Boolean isAuction) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Vehicle> vehiclesPage;
        if (isAuction != null) {
            vehiclesPage = vehicleRepository.findByVehicleStatusAndIsAuction(
                    VehicleStatus.ACTIVE,
                    isAuction ? 'Y' : 'N',
                    pageable
            );
        } else {
            vehiclesPage = vehicleRepository.findByVehicleStatusAndIsAuction(
                    VehicleStatus.ACTIVE,
                    'N',
                    pageable
            );
        }

        List<VehicleListResponse.VehicleSummary> summaries = vehiclesPage.getContent().stream()
                .map(this::buildVehicleSummary)
                .collect(Collectors.toList());

        return VehicleListResponse.builder()
                .vehicles(summaries)
                .totalCount((int) vehiclesPage.getTotalElements())
                .pageNumber(page)
                .pageSize(size)
                .build();

    }

    
    /**
     * 차량 삭제
     */
    @Transactional
    public void deleteVehicle(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("해당 차량을 찾을 수 없습니다: " + vehicleId));
        
        // 판매자 확인
        if (!vehicle.getSeller().getId().equals(userId)) {
            throw new BadRequestException("자신이 등록한 차량만 삭제할 수 있습니다.");
        }

        // TODO: 경매가 진행 중인 경우 삭제 불가 처리 추가
        // 거래 중이거나 경매 등록된 차량 삭제 불가
        if (vehicle.getVehicleStatus() != VehicleStatus.ACTIVE) {
            throw new BadRequestException("거래 중이거나 경매 등록된 차량은 삭제할 수 없습니다.");
        }
        
        vehicleRepository.delete(vehicle);
    }

    // 유틸리티

//    // 차량 상태 변경
//    @Transactional
//    public void updateVehicleStatus(Long vehicleId, VehicleStatus status) {
//        Vehicle vehicle = vehicleRepository.findById(vehicleId)
//                .orElseThrow(() -> new NotFoundException("차량을 찾을 수 없습니다."));
//
//        vehicle.setVehicleStatus(status);
//        vehicleRepository.save(vehicle);
//    }

    //시간 파싱

    private LocalDateTime parseDateTime(String dateString, boolean isStartDate) {
        try {
            // 정확한 ISO-8601 형식인 경우 그대로 파싱
            if (dateString.contains("T") && dateString.length() > 10) {
                return LocalDateTime.parse(dateString);
            }

            // 날짜만 있는 경우 (yyyy-MM-dd)
            LocalDate date = LocalDate.parse(dateString);
            if (isStartDate) {
                // 시작일은 해당 날짜의 00:00:00
                return date.atStartOfDay();
            } else {
                // 종료일은 해당 날짜의 23:59:59
                return date.atTime(LocalTime.of(23, 59, 59));
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("날짜 형식이 올바르지 않습니다: " + dateString);
        }
    }

    
    private VehicleListResponse.VehicleSummary buildVehicleSummary(Vehicle vehicle) {

        // 차량 옵션 조회
        List<String> options = vehicleOptionService.getVehicleOptionNames(vehicle);

        // 메인 옵션 (최대 3개까지)
        List<String> mainOptions = options.size() > 3 ? options.subList(0, 3) : options;

        // 차량 타입이 null일 경우 처리
        String vehicleTypeName = (vehicle.getVehicleType() != null) ? vehicle.getVehicleType().getDisplayName() : "미정";

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
                .build();
    }


    /**
     * 필터링 조건으로 차량 검색
     */
    public VehicleListResponse searchByFilter(VehicleSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Vehicle> vehiclesPage = vehicleRepository.searchByFilter(request, pageable);

        // 페이지 객체에서 차량 목록 추출 및 변환
        List<VehicleListResponse.VehicleSummary> summaries = vehiclesPage.getContent().stream()
                .map(this::buildVehicleSummary)
                .collect(Collectors.toList());

        // 차량 목록 API와 동일한 형식으로 응답 생성
        return VehicleListResponse.builder()
                .vehicles(summaries)
                .totalCount((int) vehiclesPage.getTotalElements())
                .pageNumber(request.getPage())
                .pageSize(request.getSize())
                .build();
    }

    /**
     * 카테고리별(국산/수입) 제조사 및 차량 수 조회
     */
    public List<ManufacturerCategoryResponse> getCategorizedManufacturerCounts() {
        // 1. 국산 제조사 목록 조회
        List<String> domesticManufacturers = carModelService.getDomesticManufacturers();

        // 2. 수입 제조사 목록 조회
        List<String> importedManufacturers = carModelService.getImportedManufacturers();

        // 3. 모든 제조사의 차량 수 조회
        List<ManufacturerCountResponse> allManufacturerCounts =
                vehicleRepository.countByManufacturer(VehicleStatus.ACTIVE);

        // 4. 제조사별 차량 수를 Map으로 변환하여 조회 효율 높이기
        Map<String, Long> manufacturerCountMap = allManufacturerCounts.stream()
                .collect(Collectors.toMap(
                        ManufacturerCountResponse::getManufacturer,
                        ManufacturerCountResponse::getCount
                ));

        // 5. 국산 제조사 차량 수 목록 생성
        List<ManufacturerCountResponse> domesticCounts = domesticManufacturers.stream()
                .map(manufacturer -> new ManufacturerCountResponse(
                        manufacturer,
                        manufacturerCountMap.getOrDefault(manufacturer, 0L)
                ))
                .collect(Collectors.toList());

        // 6. 수입 제조사 차량 수 목록 생성
        List<ManufacturerCountResponse> importedCounts = importedManufacturers.stream()
                .map(manufacturer -> new ManufacturerCountResponse(
                        manufacturer,
                        manufacturerCountMap.getOrDefault(manufacturer, 0L)
                ))
                .collect(Collectors.toList());

        // 7. 카테고리별 응답 생성
        List<ManufacturerCategoryResponse> result = new ArrayList<>();
        result.add(new ManufacturerCategoryResponse("국산", domesticCounts));
        result.add(new ManufacturerCategoryResponse("수입", importedCounts));

        return result;
    }

    /**
     * 특정 제조사의 모든 차명과 그에 해당하는 차량 수 조회
     */
    public List<CarNameCountResponse> getAllCarNameCounts(String manufacturer) {
        // basiccar에서 해당 제조사의 모든 차명 목록 조회
        List<String> allCarNames = carModelService.getCarNamesByManufacturer(manufacturer);

        // vehicle에서 해당 제조사의 차명별 차량 수 조회
        List<CarNameCountResponse> vehicleCounts = vehicleRepository.countByCarName(manufacturer, VehicleStatus.ACTIVE);

        // vehicleCounts를 Map으로 변환
        Map<String, Long> countsMap = vehicleCounts.stream()
                .collect(Collectors.toMap(CarNameCountResponse::getCarName, CarNameCountResponse::getCount));

        // 모든 차명에 대해 응답 생성
        return allCarNames.stream()
                .map(carName -> {
                    Long count = countsMap.getOrDefault(carName, 0L);
                    return new CarNameCountResponse(carName, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 제조사와 차명의 모든 차모델과 그에 해당하는 차량 수 조회
     */
    public List<CarModelCountResponse> getAllCarModelCounts(String manufacturer, String carName) {
        // basiccar에서 해당 제조사와 차명의 모든 차모델 목록 조회
        List<String> allCarModels = carModelService.getCarModelsByManufacturerAndCarName(manufacturer, carName);

        // vehicle에서 해당 제조사와 차명의 차모델별 차량 수 조회
        List<CarModelCountResponse> vehicleCounts = vehicleRepository.countByCarModel(manufacturer, carName, VehicleStatus.ACTIVE);

        // vehicleCounts를 Map으로 변환
        Map<String, Long> countsMap = vehicleCounts.stream()
                .collect(Collectors.toMap(CarModelCountResponse::getCarModel, CarModelCountResponse::getCount));

        // 모든 차모델에 대해 응답 생성
        return allCarModels.stream()
                .map(carModel -> {
                    Long count = countsMap.getOrDefault(carModel, 0L);
                    return new CarModelCountResponse(carModel, count);
                })
                .collect(Collectors.toList());
    }
}