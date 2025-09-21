package com.trever.backend.api.recent.repository;

import com.trever.backend.api.recent.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {
    List<RecentView> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserIdAndVehicleId(Long userId, Long vehicleId);

    Optional<RecentView> findByUserIdAndVehicleId(Long userId, Long vehicleId);
}
