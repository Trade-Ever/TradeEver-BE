package com.trever.backend.basiccar.service;
import com.trever.backend.basiccar.repository.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository repository;

    public List<String> getManufacturers(String category) {
        return repository.findDistinctManufacturerByCategory(category);
    }

    public List<String> getCarNames(String category, String manufacturer) {
        return repository.findDistinctCarNameByCategoryAndManufacturer(category, manufacturer);
    }

    public List<String> getModelNames(String category, String manufacturer, String carName) {
        return repository.findDistinctModelNameByCategoryAndManufacturerAndCarName(category, manufacturer, carName);
    }

    public List<Integer> getYears(String category, String manufacturer, String carName, String modelName) {
        return repository.findDistinctYearByCategoryAndManufacturerAndCarNameAndModelName(category, manufacturer, carName, modelName);
    }

    /**
     * 모든 제조사 목록 조회
     */
    public List<String> getAllManufacturers() {
        return repository.findDistinctManufacturers();
    }

    /**
     * 특정 제조사의 모든 차명 목록 조회
     */
    public List<String> getCarNamesByManufacturer(String manufacturer) {
        return repository.findDistinctCarNamesByManufacturer(manufacturer);
    }

    /**
     * 특정 제조사와 차명의 모든 차모델 목록 조회
     */
    public List<String> getCarModelsByManufacturerAndCarName(String manufacturer, String carName) {
        return repository.findDistinctCarModelsByManufacturerAndCarName(manufacturer, carName);
    }

    /**
     * 국산 제조사 목록 조회
     */
    public List<String> getDomesticManufacturers() {
        return repository.findDistinctDomesticManufacturers();
    }

    /**
     * 수입 제조사 목록 조회
     */
    public List<String> getImportedManufacturers() {
        return repository.findDistinctImportedManufacturers();
    }

    /**
     * 제조사가 국산인지 확인
     */
    public boolean isDomesticManufacturer(String manufacturer) {
        return getDomesticManufacturers().contains(manufacturer);
    }

    /**
     * 제조사의 카테고리(국산/수입) 반환
     */
    public String getManufacturerCategory(String manufacturer) {
        return isDomesticManufacturer(manufacturer) ? "국산" : "수입";
    }


}
