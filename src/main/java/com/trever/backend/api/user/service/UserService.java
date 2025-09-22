package com.trever.backend.api.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.trever.backend.api.jwt.JwtProvider;
import com.trever.backend.api.user.dto.*;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserProfile;
import com.trever.backend.api.user.entity.UserWallet;
import com.trever.backend.api.user.repository.UserProfileRepository;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.api.user.repository.UserWalletRepository;
import com.trever.backend.common.config.firebase.FirebaseStorageService;
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
import org.springframework.web.multipart.MultipartFile;
import org.threeten.bp.format.DateTimeParseException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserWalletService userWalletService;
    private final JwtProvider jwtProvider;
    private final UserWalletRepository userWalletRepository;
    private final GoogleOAuthService googleOAuthService;
    private final FirebaseStorageService firebaseStorageService;

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

    @Transactional
    public TokenResponseDTO loginWithGoogleIdToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }

        GoogleIdToken.Payload payload = googleOAuthService.verifyIdToken(idTokenString);

        String sub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // 기존 사용자 조회 (이 프로젝트에서는 email 기준으로 사용)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // 신규 유저 생성
            String randomPw = UUID.randomUUID().toString();
            String encodedPw = passwordEncoder.encode(randomPw);

            User newUser = User.builder()
                    .email(email)
                    .password(encodedPw)
                    .name(name)
                    .build();

            User saved = userRepository.save(newUser);

            // 프로필 생성 (필드/빌더는 엔티티에 맞추세요)
            UserProfile profile = UserProfile.builder()
                    .user(saved)
                    .profileImageUrl(picture)
                    .build();
            userProfileRepository.save(profile);

            // 지갑 생성
            userWalletService.createUserWallet(saved.getId());

            return saved;
        });

        // 인증 객체 생성 (권한 ROLE_USER)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // JWT 발급 (기존 login()과 동일하게)
        String accessToken = jwtProvider.generateAccessToken(auth);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

        // DB에 refreshToken 저장
        user.updateRefreshtoken(refreshToken);
        userRepository.save(user);

        // --- 프로필 완성도 검사 ---
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        List<String> missing = new ArrayList<>();

        // 이름 검사
        if (user.getName() == null || user.getName().isBlank()) {
            missing.add("name");
        }

        // 전화번호 검사 (User.phone)
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            missing.add("phone");
        }

        // 생년월일 검사 (UserProfile.birthDate)
        if (profile == null || profile.getBirthDate() == null) {
            missing.add("birthDate");
        }

        // 주소/도시 검사 (UserProfile.locationCity)
        if (profile == null || profile.getLocationCity() == null || profile.getLocationCity().isBlank()) {
            missing.add("locationCity");
        }

        boolean profileComplete = missing.isEmpty();

        return TokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profileComplete(profileComplete)
                .build();
    }

    @Transactional
    public void completeUserProfile(String email, UserCompleteRequestDTO req, MultipartFile profileImage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 이름 업데이트
        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }

        // 전화번호 업데이트
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            user.setPhone(req.getPhone());
        }

        // UserProfile
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_PROFILE_NOT_FOUND.getMessage()));

        if (req.getLocationCity() != null && !req.getLocationCity().isBlank()) {
            profile.setLocationCity(req.getLocationCity());
        }

        if (req.getBirthDate() != null && !req.getBirthDate().isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(req.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                profile.setBirthDate(parsed);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("birthDate must be yyyy-MM-dd");
            }
        }

        // 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // 새 이미지 업로드
                String imageUrl = firebaseStorageService.uploadImage(profileImage, "profiles");
                profile.setProfileImageUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 업로드에 실패했습니다.", e);
            }
        }

        // save
        userRepository.save(user);
        userProfileRepository.save(profile);
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
    public void updateUser(String email, UserUpdateRequestDTO userUpdateRequestDTO, MultipartFile profileImage) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_PROFILE_NOT_FOUND.getMessage()));

        if (userUpdateRequestDTO.getName() != null) {
            user.setName(userUpdateRequestDTO.getName());
        }

        if (userUpdateRequestDTO.getPhone() != null) {
            user.setPhone(userUpdateRequestDTO.getPhone());
        }

        if (userUpdateRequestDTO.getBirthDate() != null && !userUpdateRequestDTO.getBirthDate().isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(userUpdateRequestDTO.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                profile.setBirthDate(parsed);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("birthDate must be yyyy-MM-dd");
            }
        }

        if(userUpdateRequestDTO.getLocationCity() != null) {
            profile.setLocationCity(userUpdateRequestDTO.getLocationCity());
        }

        // 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // 기존 프로필 이미지가 있으면 삭제
                if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                    firebaseStorageService.deleteImage(profile.getProfileImageUrl());
                }

                // 새 이미지 업로드
                String imageUrl = firebaseStorageService.uploadImage(profileImage, "profiles");
                profile.setProfileImageUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 업로드에 실패했습니다.", e);
            }
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
