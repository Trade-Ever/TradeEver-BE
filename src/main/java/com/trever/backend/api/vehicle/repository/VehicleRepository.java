package com.trever.backend.api.vehicle.repository;

import com.trever.backend.api.vehicle.dto.CarModelCountResponse;
import com.trever.backend.api.vehicle.dto.CarNameCountResponse;
import com.trever.backend.api.vehicle.dto.ManufacturerCountResponse;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>,VehicleRepositoryCustom {
    
    Page<Vehicle> findByIsAuction(Character isAuction, Pageable pageable);
    
    Page<Vehicle> findBySellerId(Long sellerId, Pageable pageable);

    Page<Vehicle> findByVehicleStatusAndIsAuction(VehicleStatus status, char isAuction, Pageable pageable);

    @Modifying
    @Query("UPDATE Vehicle v SET v.vehicleStatus = :status WHERE v.id = :vehicleId")
    void updateVehicleStatus(@Param("vehicleId") Long vehicleId, @Param("status") VehicleStatus status);

    // 키워드 검색 (차명, 제조사, 모델, 설명 포함)
    @Query("SELECT v FROM Vehicle v " +
            "WHERE LOWER(v.carName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Vehicle> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 키워드 + 경매 여부
    @Query("SELECT v FROM Vehicle v " +
            "WHERE (LOWER(v.carName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND v.isAuction = :isAuction")
    Page<Vehicle> searchByKeywordAndAuction(@Param("keyword") String keyword,
                                            @Param("isAuction") Character isAuction,
                                            Pageable pageable);

    // 제조사별 차량 수 조회
    @Query("SELECT new com.trever.backend.api.vehicle.dto.ManufacturerCountResponse(v.manufacturer, COUNT(v)) " +
            "FROM Vehicle v WHERE v.vehicleStatus = :status GROUP BY v.manufacturer ORDER BY v.manufacturer")
    List<ManufacturerCountResponse> countByManufacturer(@Param("status") VehicleStatus status);

    // 특정 제조사의 차명별 차량 수 조회
    @Query("SELECT new com.trever.backend.api.vehicle.dto.CarNameCountResponse(v.carName, COUNT(v)) " +
            "FROM Vehicle v WHERE v.manufacturer = :manufacturer AND v.vehicleStatus = :status " +
            "GROUP BY v.carName ORDER BY v.carName")
    List<CarNameCountResponse> countByCarName(@Param("manufacturer") String manufacturer,
                                              @Param("status") VehicleStatus status);

    // 특정 제조사와 차명의 차모델별 차량 수 조회
    @Query("SELECT new com.trever.backend.api.vehicle.dto.CarModelCountResponse(v.model, COUNT(v)) " +
            "FROM Vehicle v WHERE v.manufacturer = :manufacturer AND v.carName = :carName AND v.vehicleStatus = :status " +
            "GROUP BY v.model ORDER BY v.vehicleStatus")
    List<CarModelCountResponse> countByCarModel(@Param("manufacturer") String manufacturer,
                                                @Param("carName") String carName,
                                                @Param("status") VehicleStatus status);
}