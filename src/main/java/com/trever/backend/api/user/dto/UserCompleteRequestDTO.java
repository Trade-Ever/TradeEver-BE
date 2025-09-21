package com.trever.backend.api.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCompleteRequestDTO {
    private String name;
    private String phone;
    private String locationCity;
    private String birthDate;
}
