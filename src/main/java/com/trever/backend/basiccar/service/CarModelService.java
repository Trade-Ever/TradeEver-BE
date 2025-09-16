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
}
