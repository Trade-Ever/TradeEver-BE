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

}
