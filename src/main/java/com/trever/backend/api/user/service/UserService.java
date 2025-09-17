package com.trever.backend.api.user.service;

import com.trever.backend.api.jwt.JwtProvider;
import com.trever.backend.api.user.dto.*;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserProfile;
import com.trever.backend.api.user.repository.UserProfileRepository;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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

        return UserResponseDTO.from(savedUser, profile);
    }

    // 로그인
    @Transactional
    public UserLoginResponseDTO login(UserLoginRequestDTO userLoginRequestDTO) {

        User user = userRepository.findByEmail(userLoginRequestDTO.getEmail())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_USER_EXCEPTION.getMessage()));

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
    public UserResponseDTO getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_USER_EXCEPTION.getMessage()));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.NOT_FOUND_USER_EXCEPTION.getMessage()));

        return UserResponseDTO.from(user, profile);
    }
}
