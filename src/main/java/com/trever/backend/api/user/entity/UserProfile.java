package com.trever.backend.api.user.entity;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 프로필 ID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 ID (FK)

    private LocalDate birthDate; // 생년월일

    @Column(length = 1000)
    private String profileImageUrl; // 프로필 사진 URL

    private String locationCity; // 거주 도시
}
