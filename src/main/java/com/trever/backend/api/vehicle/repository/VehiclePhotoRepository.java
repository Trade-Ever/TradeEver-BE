package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.entity.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, Long> {
    
    List<VehiclePhoto> findByVehicleIdOrderByOrderIndex(Long vehicleId);
    
    void deleteByVehicleId(Long vehicleId);
}