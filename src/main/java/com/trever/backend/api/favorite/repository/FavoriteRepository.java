package com.trever.backend.api.favorite.repository;

import com.trever.backend.api.favorite.entity.Favorite;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByUserAndVehicle(User user, Vehicle vehicle);

    void deleteByUserAndVehicle(User user, Vehicle vehicle);

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);

    // 사용자 ID로 찜 목록 조회
    List<Favorite> findByUserId(Long userId);

    // 사용자 ID와 차량 ID로 찜 여부 확인
    boolean existsByUserIdAndVehicleId(Long userId, Long vehicleId);


    // 사용자 ID와 차량 ID로 찜 정보 조회
    Favorite findByUserIdAndVehicleId(Long userId, Long vehicleId);
  
    void deleteByUserIdAndVehicleId(Long userId, Long vehicleId);

}
