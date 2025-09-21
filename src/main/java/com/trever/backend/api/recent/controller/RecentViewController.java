package com.trever.backend.api.recent.controller;

import com.trever.backend.api.recent.service.RecentViewService;
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

@RestController
@RequestMapping("/api/v1/recent-views")
@RequiredArgsConstructor
@Tag(name = "RecentView", description = "최근 본 차량 관련 API")
public class RecentViewController {

    private final RecentViewService recentViewService;
    private final UserRepository userRepository;

    @Operation(summary = "최근 본 차량 조회", description = "사용자의 최근 본 차량 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleListResponse.VehicleSummary>>> getRecentViews(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<VehicleListResponse.VehicleSummary> recentViews = recentViewService.getRecentViews(user.getId());

        return ApiResponse.success(SuccessStatus.RECENT_VIEW_LIST_SUCCESS, recentViews);
    }
}
