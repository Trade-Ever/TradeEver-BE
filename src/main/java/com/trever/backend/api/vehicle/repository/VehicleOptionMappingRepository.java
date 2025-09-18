package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleOptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleOptionMappingRepository extends JpaRepository<VehicleOptionMapping, Long> {
    List<VehicleOptionMapping> findByVehicle(Vehicle vehicle);
    void deleteByVehicle(Vehicle vehicle);
}