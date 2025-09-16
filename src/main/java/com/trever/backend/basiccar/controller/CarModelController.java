package com.trever.backend.basiccar.controller;

import com.trever.backend.basiccar.service.CarModelService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "CarModel", description = "기본차량정보 모델")
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarModelController {

    private final CarModelService service;

    // 1. 카테고리 → 제조사
    @Operation(summary = "제조사 목록 조회", description = "카테고리별 제조사 목록을 조회합니다.")
    @GetMapping("/manufacturers")
    public ResponseEntity<ApiResponse<List<String>>> getManufacturers(@RequestParam String category) {
        List<String> manufacturers = service.getManufacturers(category);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, manufacturers);
    }

    // 2. 카테고리+제조사 → 차명
    @Operation(summary = "차명 목록 조회", description = "카테고리와 제조사별 차명 목록을 조회합니다.")
    @GetMapping("/carnames")
    public ResponseEntity<ApiResponse<List<String>>> getCarNames(@RequestParam String category,
                                    @RequestParam String manufacturer) {
        List<String> carNames = service.getCarNames(category, manufacturer);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, carNames);
    }

    // 3. 카테고리+제조사+차명 → 모델
    @Operation(summary = "모델명 목록 조회", description = "카테고리, 제조사, 차명별 모델명 목록을 조회합니다.")
    @GetMapping("/modelnames")
    public ResponseEntity<ApiResponse<List<String>>> getModelNames(@RequestParam String category,
                                      @RequestParam String manufacturer,
                                      @RequestParam String carName) {
        List<String> modelNames = service.getModelNames(category, manufacturer, carName);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, modelNames);
    }

    // 4. 카테고리+제조사+차명+모델 → 연식
    @Operation(summary = "연식 목록 조회", description = "카테고리, 제조사, 차명, 모델명별 연식 목록을 조회합니다.")
    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<Integer>>> getYears(@RequestParam String category,
                                  @RequestParam String manufacturer,
                                  @RequestParam String carName,
                                  @RequestParam String modelName) {
        List<Integer> years = service.getYears(category, manufacturer, carName, modelName);
        return ApiResponse.success(SuccessStatus.CAR_INFO_SUCCESS, years);
    }

}
