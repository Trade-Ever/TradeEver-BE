package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.dto.VehicleSearchRequest;
import com.trever.backend.api.vehicle.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleRepositoryCustom {
    Page<Vehicle> searchByFilter(VehicleSearchRequest request, Pageable pageable);
}