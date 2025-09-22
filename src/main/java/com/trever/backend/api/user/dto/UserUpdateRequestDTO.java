package com.trever.backend.api.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {
    private String name;
    private String phone;
    private String locationCity;
    private String birthDate;
}
