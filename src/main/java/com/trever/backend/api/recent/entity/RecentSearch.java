package com.trever.backend.api.recent.entity;

import com.trever.backend.api.user.entity.User;
import com.trever.backend.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "recent_searches",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "keyword"})}
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecentSearch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String keyword;
}
