package com.trever.backend.api.user.service;

import com.trever.backend.api.jwt.JwtProvider;
import com.trever.backend.api.user.dto.*;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserProfile;
import com.trever.backend.api.user.entity.UserWallet;
import com.trever.backend.api.user.repository.UserProfileRepository;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.repository.UserWalletRepository;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserWalletService userWalletService;
    private final JwtProvider jwtProvider;
    private final UserWalletRepository userWalletRepository;

    // 회원가입
    @Transactional
    public UserResponseDTO signup(UserSignupRequestDTO userSignupRequestDTO) {

        // 이메일 중복 체크
        if (userRepository.findByEmail(userSignupRequestDTO.getEmail()).isPresent()) {
            throw new BadRequestException(ErrorStatus.ALREADY_REGISTERED_ACCOUNT_EXCEPTION.getMessage());
        }

        // 비밀번호 확인
        if (!userSignupRequestDTO.getPassword().equals(userSignupRequestDTO.getCheckedPassword())) {
            throw new BadRequestException(ErrorStatus.PASSWORD_MISMATCH_EXCEPTION.getMessage());
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(userSignupRequestDTO.getPassword());

        // User 저장
        User user = userSignupRequestDTO.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        // UserProfile 저장
        UserProfile profile = userSignupRequestDTO.toProfileEntity(savedUser);
        userProfileRepository.save(profile);

        //UserWallet 생성
        userWalletService.createUserWallet(user.getId());

        return UserResponseDTO.from(savedUser);
    }

    // 로그인
    @Transactional
    public UserLoginResponseDTO login(UserLoginRequestDTO userLoginRequestDTO) {

        User user = userRepository.findByEmail(userLoginRequestDTO.getEmail())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 비밀번호 검증
        if (!passwordEncoder.matches(userLoginRequestDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException(ErrorStatus.PASSWORD_MISMATCH_EXCEPTION.getMessage());
        }

        // 인증 객체 생성 (권한 ROLE_USER 고정)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // jwt 발급
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        // DB에 리프레시 토큰 저장
        user.updateRefreshtoken(refreshToken);
        userRepository.save(user);

        return UserLoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 재발급
    @Transactional
    public TokenResponseDTO reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Missing refresh token");
        }
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Refresh token not registered"));

        // 인증 객체 생성 (권한 ROLE_USER 고정)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 새 토큰 발급
        String newAccess = jwtProvider.generateAccessToken(auth);
        String newRefresh = jwtProvider.generateRefreshToken(user.getEmail());

        // 리프레시 토큰 rotation
        user.updateRefreshtoken(newRefresh);
        userRepository.save(user);

        return TokenResponseDTO.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .build();
    }

    // 회원 정보
    @Transactional
    public MyPageResponseDTO getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        Long balance = userWalletRepository.findByUserId(user.getId())
                .map(UserWallet::getBalance)
                .orElse(0L);

        return MyPageResponseDTO.from(user, profile, balance);
    }

    // 회원 정보 수정
    @Transactional
    public void updateUser(String email, UserUpdateRequestDTO userUpdateRequestDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_PROFILE_NOT_FOUND.getMessage()));

        if (userUpdateRequestDTO.getName() != null) {
            user.setName(userUpdateRequestDTO.getName());
        }

        if (userUpdateRequestDTO.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(userUpdateRequestDTO.getProfileImageUrl());
        }
        if (userUpdateRequestDTO.getNewPassword() != null && userUpdateRequestDTO.getCheckedPassword() != null) {
            if (!userUpdateRequestDTO.getNewPassword().equals(userUpdateRequestDTO.getCheckedPassword())) {
                throw new BadRequestException(ErrorStatus.PASSWORD_MISMATCH_EXCEPTION.getMessage());
            }
            user.setPassword(passwordEncoder.encode(userUpdateRequestDTO.getNewPassword()));
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
    }

    // 로그아웃
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        if (user.getRefreshToken() == null) {
            throw new BadRequestException(ErrorStatus.USER_ALREADY_LOGGED_OUT.getMessage());
        }

        user.updateRefreshtoken(null);
    }
}
