package com.trever.backend.api.vehicle.service;

import com.trever.backend.api.auction.dto.AuctionCreateRequest;
import com.trever.backend.api.auction.service.AuctionService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.dto.VehicleCreateRequest;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.api.vehicle.dto.VehiclePhotoDto;
import com.trever.backend.api.vehicle.dto.VehicleResponse;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuctionService auctionService;
    private final VehiclePhotoService vehiclePhotoService; // 추가: VehiclePhotoService 의존성 주입
    
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
                .vehicleStatus(request.getVehicleStatus())
                .engineCc(request.getEngineCc())
                .horsepower(request.getHorsepower())
                .color(request.getColor())
                .additionalInfo(request.getAdditionalInfo())
                .price(Boolean.FALSE.equals(request.getIsAuction()) ? request.getPrice() : null)
                .isAuction(Boolean.TRUE.equals(request.getIsAuction()) ? 'Y' : 'N')
                .locationAddress(request.getLocationAddress())
                .favoriteCount(0)
                .seller(seller)
                .build();
                
        // 차량 엔티티 저장
        vehicle = vehicleRepository.save(vehicle);
        
        // 사진 업로드 및 저장
        if (photos != null && !photos.isEmpty()) {
            vehiclePhotoService.uploadAndSaveVehiclePhotos(vehicle, photos, request.getPhotoOrders());
        }
        
        // 경매 등록이 필요한 경우
        if (Boolean.TRUE.equals(request.getIsAuction())) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            
            AuctionCreateRequest auctionRequest = AuctionCreateRequest.builder()
                    .startPrice(request.getStartPrice())
                    .startAt(LocalDateTime.parse(request.getStartAt(), formatter))
                    .endAt(LocalDateTime.parse(request.getEndAt(), formatter))
                    .vehicleId(vehicle.getId())
                    .build();
            
            Long auctionId = auctionService.createAuction(auctionRequest, vehicle);
            vehicle.setAuctionId(auctionId);
            vehicleRepository.save(vehicle);
        }
        
        return vehicle.getId();
    }
    
    /**
     * 차량 상세 조회
     */
    public VehicleResponse getVehicleDetail(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("차량을 찾을 수 없습니다."));

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
                .vehicleStatus(vehicle.getVehicleStatus())
                .engineCc(vehicle.getEngineCc())
                .horsepower(vehicle.getHorsepower())
                .color(vehicle.getColor())
                .additionalInfo(vehicle.getAdditionalInfo())
                .isAuction(vehicle.getIsAuction())
                .price(vehicle.getPrice())
                .locationAddress(vehicle.getLocationAddress())
                .representativePhotoUrl(vehicle.getRepresentativePhotoUrl()) // 대표 사진 URL 추가
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
            vehiclesPage = vehicleRepository.findByIsAuction(
                    isAuction ? 'Y' : 'N', 
                    pageable
            );
        } else {
            vehiclesPage = vehicleRepository.findAll(pageable);
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
        
        vehicleRepository.delete(vehicle);
    }
    
    // 유틸리티 메서드
    
    private VehicleResponse buildVehicleResponse(Vehicle vehicle) {
        // 차량 사진 조회
        List<VehiclePhotoDto> photos = vehiclePhotoService.getVehiclePhotos(vehicle.getId()).stream()
                .map(photo -> VehiclePhotoDto.builder()
                        .id(photo.getId())
                        .photoUrl(photo.getPhotoUrl())
                        .orderIndex(photo.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
        
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .carNumber(vehicle.getCarNumber())
                .carName(vehicle.getCarName())
                .description(vehicle.getDescription())
                .manufacturer(vehicle.getManufacturer())
                .model(vehicle.getModel())
                .year_value(vehicle.getYear_value())
                .mileage(vehicle.getMileage())
                .fuelType(vehicle.getFuelType())
                .transmission(vehicle.getTransmission())
                .accidentHistory(vehicle.getAccidentHistory())
                .accidentDescription(vehicle.getAccidentDescription())
                .vehicleStatus(vehicle.getVehicleStatus())
                .engineCc(vehicle.getEngineCc())
                .horsepower(vehicle.getHorsepower())
                .color(vehicle.getColor())
                .additionalInfo(vehicle.getAdditionalInfo())
                .price(vehicle.getPrice())
                .isAuction(vehicle.getIsAuction())
                .auctionId(vehicle.getAuctionId())
                .locationAddress(vehicle.getLocationAddress())
                .favoriteCount(vehicle.getFavoriteCount())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .sellerId(vehicle.getSeller().getId())
                .sellerName(vehicle.getSeller().getName())
                .photos(photos) // 사진 정보 추가
                .build();
    }
    
    private VehicleListResponse.VehicleSummary buildVehicleSummary(Vehicle vehicle) {
        return VehicleListResponse.VehicleSummary.builder()
                .id(vehicle.getId())
                .carNumber(vehicle.getCarNumber())
                .carName(vehicle.getCarName())
                .manufacturer(vehicle.getManufacturer())
                .model(vehicle.getModel())
                .year_value(vehicle.getYear_value())
                .mileage(vehicle.getMileage())
                .transmission(vehicle.getTransmission())
                .fuelType(vehicle.getFuelType())
                .price(vehicle.getPrice())
                .isAuction(vehicle.getIsAuction())
                .auctionId(vehicle.getAuctionId())
                .representativePhotoUrl(vehicle.getRepresentativePhotoUrl())
                .locationAddress(vehicle.getLocationAddress())
                .favoriteCount(vehicle.getFavoriteCount())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}