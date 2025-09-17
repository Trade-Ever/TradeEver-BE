package com.trever.backend.api.vehicle.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import com.trever.backend.api.vehicle.dto.VehicleCreateRequest;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.api.vehicle.dto.VehicleResponse;
import com.trever.backend.api.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Vehicle", description = "차량 관리 API")
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    
    @Operation(summary = "차량 등록", description = "새로운 차량을 등록합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createVehicle(
            @RequestPart("request") String requestString,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        
        try {
            // JSON 문자열을 객체로 변환
            VehicleCreateRequest request = objectMapper.readValue(requestString, VehicleCreateRequest.class);
            
            // TODO: 실제 구현 시에는 인증된 사용자의 ID를 사용해야 함
            Long sellerId = 1L; // 테스트용 판매자 ID
            
            Long vehicleId = vehicleService.createVehicle(request, photos, sellerId);
            return ApiResponse.success(SuccessStatus.VEHICLE_CREATED, vehicleId);
        } catch (JsonProcessingException e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("JSON 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }
    
    @Operation(summary = "차량 상세 조회", description = "차량 상세 정보를 조회합니다.")
    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleDetail(
            @PathVariable Long vehicleId) {
        
        VehicleResponse vehicle = vehicleService.getVehicleDetail(vehicleId);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, vehicle);
    }
    
    @Operation(summary = "차량 목록 조회", description = "차량 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<VehicleListResponse>> getVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean isAuction) {
        
        VehicleListResponse vehicles = vehicleService.getVehicles(page, size, sortBy, isAuction);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, vehicles);
    }
    
    @Operation(summary = "차량 삭제", description = "차량을 삭제합니다. 자신이 등록한 차량만 삭제할 수 있습니다.")
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable Long vehicleId) {
        
        // TODO: 실제 구현 시에는 인증된 사용자의 ID를 사용해야 함
        Long userId = 1L; // 테스트용 사용자 ID
        
        vehicleService.deleteVehicle(vehicleId, userId);
        return ApiResponse.success_only(SuccessStatus.CAR_INFO_SUCCESS);
    }
}