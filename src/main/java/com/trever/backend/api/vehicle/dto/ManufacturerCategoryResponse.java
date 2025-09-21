package com.trever.backend.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturerCategoryResponse {
    private String category; // "국산" 또는 "수입"
    private List<ManufacturerCountResponse> manufacturers;
}