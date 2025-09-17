package com.trever.backend.api.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePhotoDto {
    private Long id;
    private String photoUrl;
    private Integer orderIndex;
    private MultipartFile file; // 업로드용 (요청 시에만 사용)
}