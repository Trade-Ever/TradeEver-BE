package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.entity.VehicleOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleOptionRepository extends JpaRepository<VehicleOption, Long> {
    Optional<VehicleOption> findByName(String name);
    List<VehicleOption> findByNameIn(List<String> names);
}