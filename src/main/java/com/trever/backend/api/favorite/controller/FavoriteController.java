package com.trever.backend.api.favorite.controller;

import com.trever.backend.api.favorite.dto.FavoriteResponseDTO;
import com.trever.backend.api.favorite.service.FavoriteService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.vehicle.dto.VehicleListResponse;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.ErrorStatus;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorite", description = "찜 관련 API")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    // 찜 목록 조회
    @Operation(summary = "찜 목록 조회 API", description = "사용자의 찜 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<VehicleListResponse>> getFavorites(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        VehicleListResponse favorites = favoriteService.getFavorites(user.getId());

        return ApiResponse.success(SuccessStatus.GET_FAVORITE_SUCCESS, favorites);
    }

//    @Operation(summary = "찜 추가 API", description = "차량을 찜합니다.")
//    @PostMapping("/{vehicleId}")
//    public ResponseEntity<ApiResponse<Void>> addFavorite(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @PathVariable Long vehicleId) {
//
//        String email = userDetails.getUsername();
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//
//        favoriteService.addFavorite(user.getId(), vehicleId);
//        return ApiResponse.success(SuccessStatus.ADD_FAVORITE_SUCCESS, null);
//    }
//
//    @Operation(summary = "찜 취소 API", description = "찜을 취소합니다.")
//    @DeleteMapping("/{vehicleId}")
//    public ResponseEntity<ApiResponse<Void>> removeFavorite(@AuthenticationPrincipal UserDetails userDetails,
//                                                            @PathVariable Long vehicleId) {
//
//        String email = userDetails.getUsername();
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));
//
//        favoriteService.removeFavorite(user.getId(), vehicleId);
//        return ApiResponse.success(SuccessStatus.REMOVE_FAVORITE_SUCCESS, null);
//    }

    @Operation(summary = "찜 토글", description = "차량 찜을 토글합니다. (찜 → 해제, 해제 → 찜)")
    @PostMapping("/{vehicleId}/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        boolean isFavorited = favoriteService.toggleFavorite(user.getId(), vehicleId);

        // true → 찜 성공, false → 찜 해제
        return ApiResponse.success(SuccessStatus.FAVORITE_TOGGLE_SUCCESS, isFavorited);
    }

}
