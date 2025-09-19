package com.trever.backend.api.user.dto;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MyPageResponseDTO {
    private Long userId;
    private String email;
    private String name;
    private String phone;

    // user_profiles에서 가져오는 값
    private String profileImageUrl;
    private LocalDate birthDate;
    private String locationCity;

    private Long balance;

    public static MyPageResponseDTO from(User user, UserProfile profile, Long balance) {
        return MyPageResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .profileImageUrl(profile.getProfileImageUrl())
                .birthDate(profile.getBirthDate())
                .locationCity(profile.getLocationCity())
                .balance(balance)
                .build();
    }
}
