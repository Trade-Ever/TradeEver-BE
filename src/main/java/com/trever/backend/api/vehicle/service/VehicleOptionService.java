package com.trever.backend.api.vehicle.service;

import com.trever.backend.api.vehicle.entity.Vehicle;
import com.trever.backend.api.vehicle.entity.VehicleOption;
import com.trever.backend.api.vehicle.entity.VehicleOptionMapping;
import com.trever.backend.api.vehicle.repository.VehicleOptionMappingRepository;
import com.trever.backend.api.vehicle.repository.VehicleOptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleOptionService {

    private final VehicleOptionRepository optionRepository;
    private final VehicleOptionMappingRepository optionMappingRepository;

    // 기본 옵션 목록
    private static final List<String> DEFAULT_OPTIONS = Arrays.asList(
            "열선시트", "통풍시트", "썬루프", "열선핸들", "내비게이션", "전동시트", "어라운드뷰", "전동트렁크"
    );

    /**
     * 애플리케이션 시작 시 기본 옵션 데이터 초기화
     */
    @PostConstruct
    @Transactional
    public void initializeOptions() {
        // 이미 옵션 데이터가 있는지 확인
        if (optionRepository.count() > 0) {
            return;
        }

        // 기본 옵션 생성
        DEFAULT_OPTIONS.forEach(optionName -> {
            VehicleOption option = VehicleOption.builder()
                    .name(optionName)
                    .build();
            optionRepository.save(option);
        });
    }

    /**
     * 차량에 옵션 목록 설정
     */
    @Transactional
    public void setVehicleOptions(Vehicle vehicle, List<String> optionNames) {
        // 기존 옵션 매핑 제거
        optionMappingRepository.deleteByVehicle(vehicle);

        if (optionNames == null || optionNames.isEmpty()) {
            return;
        }

        // 옵션 엔티티 조회
        List<VehicleOption> options = optionRepository.findByNameIn(optionNames);

        // 새 옵션 매핑 생성
        List<VehicleOptionMapping> mappings = options.stream()
                .map(option -> VehicleOptionMapping.builder()
                        .vehicle(vehicle)
                        .option(option)
                        .build())
                .collect(Collectors.toList());

        optionMappingRepository.saveAll(mappings);
    }

    /**
     * 차량의 옵션 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getVehicleOptionNames(Vehicle vehicle) {
        return optionMappingRepository.findByVehicle(vehicle).stream()
                .map(mapping -> mapping.getOption().getName())
                .collect(Collectors.toList());
    }

    /**
     * 모든 옵션 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getAllOptionNames() {
        return optionRepository.findAll().stream()
                .map(VehicleOption::getName)
                .collect(Collectors.toList());
    }
}