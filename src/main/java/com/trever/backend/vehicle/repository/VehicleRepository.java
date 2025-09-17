package com.trever.backend.vehicle.repository;

import com.trever.backend.vehicle.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    
    Page<Vehicle> findByIsAuction(Character isAuction, Pageable pageable);
    
    Page<Vehicle> findBySellerId(Long sellerId, Pageable pageable);
}