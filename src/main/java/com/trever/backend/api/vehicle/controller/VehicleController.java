package com.trever.backend.api.vehicle.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trever.backend.api.recent.service.RecentSearchService;
import com.trever.backend.api.recent.service.RecentViewService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.dto.*;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import com.trever.backend.api.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Vehicle", description = "차량 관리 API")
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RecentViewService recentViewService;
    private final RecentSearchService recentSearchService;
    
    @Operation(summary = "차량 등록", description = "새로운 차량을 등록합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createVehicle(
            @RequestPart("request") String requestString,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // JSON 문자열을 객체로 변환
            VehicleCreateRequest request = objectMapper.readValue(requestString, VehicleCreateRequest.class);
            
            String email = userDetails.getUsername();
            User seller = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
            
            Long vehicleId = vehicleService.createVehicle(request, photos, seller.getId());
            return ApiResponse.success(SuccessStatus.VEHICLE_CREATED, vehicleId);

        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("입력 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }
    
    @Operation(summary = "차량 상세 조회", description = "차량 상세 정보를 조회합니다.")
    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleDetail(
            @PathVariable("vehicleId") Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long loginUserId = null;
        String email = null;
        if (userDetails != null) {
            email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
            loginUserId = user.getId();
        }

        log.info("loginUserId={}, email={}", loginUserId, email);

        VehicleResponse vehicle = vehicleService.getVehicleDetail(vehicleId, loginUserId);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, vehicle);
    }
    
    @Operation(summary = "차량 목록", description = "차량 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<VehicleListResponse>> getVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean isAuction,
            @AuthenticationPrincipal UserDetails userDetails
            ) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
        
        VehicleListResponse vehicles = vehicleService.getVehicles(page, size, sortBy, isAuction, user.getId());
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, vehicles);
    }
    
    @Operation(summary = "차량 삭제", description = "차량을 삭제합니다. 자신이 등록한 차량만 삭제할 수 있습니다.")
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        vehicleService.deleteVehicle(vehicleId, user.getId());
        return ApiResponse.success_only(SuccessStatus.CAR_INFO_SUCCESS);
    }

//    @PatchMapping("/{vehicleId}/status")
//    @Operation(summary = "차량 상태 변경", description = "차량의 판매 상태를 변경합니다.")
//    public ResponseEntity<ApiResponse<String>> updateVehicleStatus(
//            @PathVariable Long vehicleId,
//            @RequestParam String stauts) {
//
//        VehicleStatus changeStatus = VehicleStatus.valueOf(stauts);
//
//        vehicleService.updateVehicleStatus(vehicleId, changeStatus);
//        return ApiResponse.success(SuccessStatus.CAR_TYPE_GET_SEUCESS, "차량 상태가 변경되었습니다.");
//    }
//
//    @GetMapping("/status")
//    @Operation(summary = "차량 상태 목록 조회", description = "사용 가능한 차량 상태 목록을 조회합니다.")
//    public ResponseEntity<ApiResponse<VehicleStatus[]>> getVehicleStatuses() {
//        return ApiResponse.success(SuccessStatus.BID_SUCESS, VehicleStatus.values());
//    }

    /**
     * 내가 등록한 차량 목록 조회
     */
    @GetMapping("/my-vehicles")
    @Operation(summary = "내가 등록한 차량 목록조회", description = "내가 등록한 차량 목록조회")
    public ResponseEntity<ApiResponse<VehicleListResponse>> getMyVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        VehicleListResponse response = vehicleService.getMyVehicles(user.getId(), page, size, sortBy);
        
        return ApiResponse.success(SuccessStatus.READ_MY_VEHICLE_SUCCESS, response);
    }

    /**
     * 차량번호 존재 여부 확인 API
     */
    @GetMapping("/check-car-number")
    public ResponseEntity<ApiResponse<CarNumberExistsResponse>> checkCarNumberExists(
            @RequestParam String carNumber) {

        CarNumberExistsResponse response = vehicleService.checkCarNumberExists(carNumber);

        return ApiResponse.success(SuccessStatus.CHECK_CAR_NUMBER_SUCCESS,response);
    }


    /**
     * 필터링 조건으로 차량 검색
     */
    @PostMapping("/search")
    @Operation(summary = "차량 검색", description = "차량을 검색합니다.")
    public ResponseEntity<ApiResponse<VehicleListResponse>> searchVehicles(
            @RequestBody VehicleSearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        VehicleListResponse result = vehicleService.searchByFilter(request);

        Long loginUserId = null;
        if (userDetails != null && request.getKeyword() != null) {
            String email = userDetails.getUsername();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
            loginUserId = user.getId();

            // 최근 검색어 저장
            recentSearchService.addSearch(loginUserId, request.getKeyword());
        }


        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, result);
    }

    /**
     * 국산/수입별 제조사 및 차량 수 조회
     */
    @GetMapping("/manufacturers")
    @Operation(summary = "제조사별 차량 수 조회", description = "제조사별 차량 수 조회")
    public ResponseEntity<ApiResponse<List<ManufacturerCategoryResponse>>> getCategorizedManufacturers() {
        List<ManufacturerCategoryResponse> result = vehicleService.getCategorizedManufacturerCounts();
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, result);
    }

    /**
     * 특정 제조사의 모든 차명과 차량 수 조회
     */
    @GetMapping("/manufacturers/{manufacturer}/car-names")
    @Operation(summary = "제조사별, 차명 별 차량 수 조회", description = "제조사별, 차명 별 차량 수 조회")
    public ResponseEntity<ApiResponse<List<CarNameCountResponse>>> getCarNames(
            @PathVariable String manufacturer) {
        List<CarNameCountResponse> result = vehicleService.getAllCarNameCounts(manufacturer);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, result);
    }

    /**
     * 특정 제조사와 차명의 모든 차모델과 차량 수 조회
     */
    @GetMapping("/manufacturers/{manufacturer}/car-names/{carName}/car-models")
    @Operation(summary = "제조사별, 차명,차 모델 별 차량 수 조회", description = "제조사별, 차명, 차 모델 별 차량 수 조회")
    public ResponseEntity<ApiResponse<List<CarModelCountResponse>>> getCarModels(
            @PathVariable String manufacturer,
            @PathVariable String carName) {
        List<CarModelCountResponse> result = vehicleService.getAllCarModelCounts(manufacturer, carName);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, result);
    }
}