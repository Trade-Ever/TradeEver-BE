package com.trever.backend.api.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ApiException;
import com.trever.backend.api.user.dto.*;
import com.trever.backend.api.user.service.UserService;
import com.trever.backend.common.response.ApiResponse;
import com.trever.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/users")
@Tag(name="User", description = "User 관련 API 입니다.")
public class UserController {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    // 회원가입
    @Operation(summary = "회원가입 API", description = "회원정보를 받아 사용자를 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponseDTO>> signup(@RequestBody UserSignupRequestDTO userSignupRequestDTO) {
        UserResponseDTO response = userService.signup(userSignupRequestDTO);
        return ApiResponse.success(SuccessStatus.SEND_REGISTER_SUCCESS, response);
    }

    @PostMapping("/auth/google/login")
    public ResponseEntity<ApiResponse<TokenResponseDTO>> googleLogin(@RequestBody GoogleLoginRequest req) {
        String idToken = req.getIdToken();
        TokenResponseDTO tokens = userService.loginWithGoogleIdToken(idToken);
        return ApiResponse.success(SuccessStatus.SEND_LOGIN_SUCCESS, tokens);
    }

    @Operation(summary = "회원 추가 정보 입력(완성) API", description = "로그인한 사용자가 전화번호/주소/생년월일/이름 등 누락된 정보를 채웁니다.")
    @PostMapping("/me/complete")
    public ResponseEntity<ApiResponse<Void>> completeProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserCompleteRequestDTO req) {



        String email = userDetails.getUsername();
        userService.completeUserProfile(email, req);

        return ApiResponse.success_only(SuccessStatus.UPDATE_PROFILE_SUCCESS);
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

    @Operation(
            summary = "프로필 수정 API",
            description = "로그인한 사용자의 프로필 정보를 수정합니다. 프로필 정보나 이미지 중 하나만 업데이트할 수도 있습니다.\n\n" +
                    "요청 예시:\n" +
                    "```\n" +
                    "Content-Type: multipart/form-data\n\n" +
                    "{\n" +
                    "  \"name\": \"홍길동\",\n" +
                    "  \"phone\": \"010-1234-5678\",\n" +
                    "  \"locationCity\": \"서울시 강남구\",\n" +
                    "  \"birthDate\": \"yyyy-MM-dd\"\n" +
                    "}\n" +
                    "profileImage: (이미지 파일)\n" +
                    "```"
    )
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "userInfo", required = false) String userUpdateJson,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        try {
            String email = userDetails.getUsername();
            UserUpdateRequestDTO userUpdateRequestDTO = null;

            // userInfo가 제공된 경우에만 JSON 파싱 시도
            if (userUpdateJson != null && !userUpdateJson.trim().isEmpty()) {
                userUpdateRequestDTO = objectMapper.readValue(userUpdateJson, UserUpdateRequestDTO.class);
            } else {
                // userInfo가 없는 경우 빈 객체 생성
                userUpdateRequestDTO = new UserUpdateRequestDTO();
            }

            // 이미지만 있거나, 정보만 있거나, 둘 다 있는 경우 처리
            userService.updateUser(email, userUpdateRequestDTO, profileImage);

            return ApiResponse.success_only(SuccessStatus.UPDATE_PROFILE_SUCCESS);
        } catch (Exception e) {
            log.error("프로필 업데이트 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("프로필 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "로그아웃 API", description = "사용자가 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername(); // JWT에서 꺼낸 이메일
        userService.logout(email);
        return ApiResponse.success_only(SuccessStatus.SEND_LOGOUT_SUCCESS);
    }
}
