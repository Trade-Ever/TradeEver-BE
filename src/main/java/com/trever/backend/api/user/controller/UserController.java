package com.trever.backend.api.user.controller;

import com.trever.backend.api.user.dto.*;
import com.trever.backend.api.user.service.UserService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/users")
@Tag(name="User", description = "User 관련 API 입니다.")
public class UserController {

    private final UserService userService;

    // 회원가입
    @Operation(summary = "회원가입 API", description = "회원정보를 받아 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDTO>> signup(@RequestBody UserSignupRequestDTO userSignupRequestDTO) {
        UserResponseDTO response = userService.signup(userSignupRequestDTO);
        return ApiResponse.success(SuccessStatus.SEND_REGISTER_SUCCESS, response);
    }

    // 로그인
    @Operation(summary = "로그인 API", description = "이메일로 로그인을 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponseDTO>> login(@RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        UserLoginResponseDTO response = userService.login(userLoginRequestDTO);

        return ApiResponse.success(SuccessStatus.SEND_LOGIN_SUCCESS, response);
    }

    // 토큰 재발급
    @Operation(summary = "토큰 재발급", description = "Refresh 토큰을 이용해 Access / Refresh 토큰을 재발급합니다. (Header: Authorization-Refresh: Bearer {token})")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> reissue(
            @RequestBody TokenRequestDTO tokenRequestDTO) {
        TokenResponseDTO response = userService.reissue(tokenRequestDTO.getRefreshToken());

        return ApiResponse.success(SuccessStatus.SEND_LOGIN_SUCCESS, response);
    }

    // 사용자 정보
    @Operation(summary = "사용자 정보 조회 API", description = "사용자 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPageResponseDTO>> getMemberInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        MyPageResponseDTO myPageResponseDTO = userService.getMyInfo(email);

        return ApiResponse.success(SuccessStatus.SEND_MEMBER_SUCCESS, myPageResponseDTO);
    }


}
