package com.trever.backend.api.recent.controller;

import com.trever.backend.api.recent.service.RecentSearchService;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
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
@RequestMapping("/api/v1/recent-searches")
@RequiredArgsConstructor
@Tag(name = "RecentSearch", description = "최근 검색어 관련 API")
public class RecentSearchController {

    private final RecentSearchService recentSearchService;
    private final UserRepository userRepository;

    @Operation(summary = "최근 검색어 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getRecentSearches(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<String> searches = recentSearchService.getRecentSearches(user.getId());
        return ApiResponse.success(SuccessStatus.RECENT_SEARCH_LIST_SUCCESS, searches);
    }

    @Operation(summary = "최근 검색어 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeSearch(
            @RequestParam String keyword,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        recentSearchService.removeSearch(user.getId(), keyword);
        return ApiResponse.success(SuccessStatus.RECENT_SEARCH_DELETE_SUCCESS, null);
    }
}
