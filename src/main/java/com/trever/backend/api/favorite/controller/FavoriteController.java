package com.trever.backend.api.favorite.controller;

import com.trever.backend.api.favorite.dto.FavoriteResponseDTO;
import com.trever.backend.api.favorite.service.FavoriteService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorite", description = "찜 관련 API")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 찜 목록 조회
    @Operation(summary = "찜 목록 조회 API", description = "사용자의 찜 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponseDTO>>> getFavorites(@RequestParam Long userId) {
        List<FavoriteResponseDTO> favorites = favoriteService.getFavorites(userId);

        return ApiResponse.success(SuccessStatus.GET_FAVORITE_SUCCESS, favorites);
    }

    @Operation(summary = "찜 추가 API", description = "차량을 찜합니다.")
    @PostMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(@RequestParam Long userId,
                                                         @PathVariable Long vehicleId) {
        favoriteService.addFavorite(userId, vehicleId);
        return ApiResponse.success(SuccessStatus.ADD_FAVORITE_SUCCESS, null);
    }

    @Operation(summary = "찜 취소 API", description = "찜을 취소합니다.")
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@RequestParam Long userId,
                                                            @PathVariable Long vehicleId) {
        favoriteService.removeFavorite(userId, vehicleId);
        return ApiResponse.success(SuccessStatus.REMOVE_FAVORITE_SUCCESS, null);
    }
}
