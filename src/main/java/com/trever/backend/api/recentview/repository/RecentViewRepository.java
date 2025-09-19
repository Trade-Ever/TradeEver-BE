package com.trever.backend.api.recentview.repository;

import com.trever.backend.api.recentview.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {
    List<RecentView> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<RecentView> findByUserIdAndVehicleId(Long userId, Long vehicleId);
}
