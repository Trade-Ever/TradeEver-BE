package com.trever.backend.api.vehicle.controller;

import com.trever.backend.api.vehicle.entity.VehicleType;
import com.trever.backend.api.vehicle.service.VehicleOptionService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Vehicle Options", description = "차량 옵션 API")
@RequestMapping("/api/vehicle-options")
@RequiredArgsConstructor
public class VehicleOptionController {

    private final VehicleOptionService vehicleOptionService;

    @GetMapping
    @Operation(summary = "차량 옵션 목록 조회", description = "사용 가능한 모든 차량 옵션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<String>>> getAllOptions() {
        List<String> options = vehicleOptionService.getAllOptionNames();
        return ApiResponse.success(SuccessStatus.CAR_OPTION_GET_SEUCESS, options);
    }

    @GetMapping("/types")
    @Operation(summary = "차종 목록 조회", description = "모든 차종 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAllVehicleTypes() {
        List<Map<String, String>> types = Arrays.stream(VehicleType.values())
                .map(type -> Map.of(
                        "code", type.name(),
                        "name", type.getDisplayName()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(SuccessStatus.CAR_TYPE_GET_SEUCESS, types);
    }
}