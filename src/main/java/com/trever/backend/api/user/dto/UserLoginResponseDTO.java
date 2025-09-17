package com.trever.backend.api.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginResponseDTO {
    private String accessToken;
    private String refreshToken;
}
