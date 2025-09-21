package com.trever.backend.api.vehicle.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.trever.backend.api.vehicle.dto.VehicleSearchRequest;
import com.trever.backend.api.vehicle.entity.QVehicle;
import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleStatus;
import com.trever.backend.api.vehicle.entity.VehicleType;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

public class VehicleRepositoryImpl implements VehicleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public VehicleRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Vehicle> searchByFilter(VehicleSearchRequest request, Pageable pageable) {
        QVehicle vehicle = QVehicle.vehicle;

        // 기본 조건: 차량 상태가 ACTIVE인 것만 조회
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(vehicle.vehicleStatus.eq(VehicleStatus.ACTIVE));

        // 1. 키워드 검색 (제목 포함)
        if (StringUtils.hasText(request.getKeyword())) {
            builder.and(vehicle.carName.containsIgnoreCase(request.getKeyword()));
        }

        // 2. 차모델 필터링
        if (StringUtils.hasText(request.getManufacturer())) {
            builder.and(vehicle.manufacturer.eq(request.getManufacturer()));
        }

        if (StringUtils.hasText(request.getCarName())) {
            builder.and(vehicle.carName.eq(request.getCarName()));
        }

        if (StringUtils.hasText(request.getCarModel())) {
            builder.and(vehicle.model.eq(request.getCarModel()));
        }

        // 3. 연식 필터링
        if (request.getYearStart() != null) {
            builder.and(vehicle.year_value.goe(request.getYearStart()));
        }

        if (request.getYearEnd() != null) {
            builder.and(vehicle.year_value.loe(request.getYearEnd()));
        }

        // 4. 주행거리 필터링
        if (request.getMileageStart() != null) {
            builder.and(vehicle.mileage.goe(request.getMileageStart()));
        }

        if (request.getMileageEnd() != null) {
            builder.and(vehicle.mileage.loe(request.getMileageEnd()));
        }

        // 5. 가격 필터링
        if (request.getPriceStart() != null) {
            builder.and(vehicle.price.goe(request.getPriceStart()));
        }

        if (request.getPriceEnd() != null) {
            builder.and(vehicle.price.loe(request.getPriceEnd()));
        }

        // 6. 차종 필터링
        if (StringUtils.hasText(request.getVehicleType())) {
            builder.and(vehicle.vehicleType.eq(VehicleType.valueOf(request.getVehicleType())));
        }

        // 조회 쿼리 생성
        JPAQuery<Vehicle> query = queryFactory
                .selectFrom(vehicle)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 정렬 적용
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (order.getProperty().equals("price")) {
                    if (order.isAscending()) {
                        query.orderBy(vehicle.price.asc());
                    } else {
                        query.orderBy(vehicle.price.desc());
                    }
                } else if (order.getProperty().equals("createdAt")) {
                    if (order.isAscending()) {
                        query.orderBy(vehicle.createdAt.asc());
                    } else {
                        query.orderBy(vehicle.createdAt.desc());
                    }
                }
            });
        } else {
            // 기본 정렬은 최신순
            query.orderBy(vehicle.createdAt.desc());
        }

        // 결과 조회
        List<Vehicle> vehicles = query.fetch();

        // 전체 개수 조회
        long total = queryFactory
                .selectFrom(vehicle)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(vehicles, pageable, total);
    }
}