package com.trever.backend.api.favorite.repository;

import com.trever.backend.api.favorite.entity.Favorite;
import com.trever.backend.api.recentview.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);

    boolean existsByUserIdAndVehicleId(Long userId, Long vehicleId);

    void deleteByUserIdAndVehicleId(Long userId, Long vehicleId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);

}
