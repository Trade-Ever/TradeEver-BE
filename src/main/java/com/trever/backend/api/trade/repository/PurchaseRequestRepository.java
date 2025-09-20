package com.trever.backend.api.trade.repository;

import com.trever.backend.api.trade.entity.PurchaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseApplication, Long> {
    List<PurchaseApplication> findByVehicleId(Long vehicleId);
    boolean existsByVehicleIdAndBuyerId(Long vehicleId, Long buyerId);
}