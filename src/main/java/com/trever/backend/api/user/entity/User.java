package com.trever.backend.api.user.entity;

import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    private String password;
    
    private String name;
    
    private String phone;

    private String refreshToken;

    // 리프레시 토큰 업데이트
    public void updateRefreshtoken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
