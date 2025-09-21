package com.trever.backend.api.recent.repository;

import com.trever.backend.api.recent.entity.RecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {
    List<RecentSearch> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserIdAndKeyword(Long userId, String keyword);
}
