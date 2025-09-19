package com.trever.backend.api.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {
    private String name;
    private String profileImageUrl;
    private String newPassword;
    private String checkedPassword;
}
