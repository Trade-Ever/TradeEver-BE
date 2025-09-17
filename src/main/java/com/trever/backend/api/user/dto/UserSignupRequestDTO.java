package com.trever.backend.api.user.dto;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.entity.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserSignupRequestDTO {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "비밀번호는 영문 대소문자, 숫자, 특수문자를 포함해 8자 이상이어야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String checkedPassword;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(
            regexp = "^01[0-9][0-9]?[-]?[0-9]{3,4}[-]?[0-9]{4}$",
            message = "전화번호 형식이 올바르지 않습니다. 예: 01012345678 또는 010-1234-5678"
    )
    private String phone;

    @NotNull(message = "생년월일을 입력해주세요.")
    private LocalDate birthDate;

    private String profileImageUrl;
    private String locationCity;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .phone(phone)
                .build();
    }

    public UserProfile toProfileEntity(User user) {
        return UserProfile.builder()
                .birthDate(birthDate)
                .profileImageUrl(profileImageUrl)
                .locationCity(locationCity)
                .user(user)
                .build();
    }
}
