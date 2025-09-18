package com.trever.backend.api.user.dto;

import lombok.*;

@Getter
@Builder
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
}
