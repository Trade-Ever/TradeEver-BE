package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    Page<Vehicle> findByIsAuction(Character isAuction, Pageable pageable);
    
    Page<Vehicle> findBySellerId(Long sellerId, Pageable pageable);

    @Modifying
    @Query("UPDATE Vehicle v SET v.vehicleStatus = :status WHERE v.id = :vehicleId")
    void updateVehicleStatus(@Param("vehicleId") Long vehicleId, @Param("status") VehicleStatus status);

}