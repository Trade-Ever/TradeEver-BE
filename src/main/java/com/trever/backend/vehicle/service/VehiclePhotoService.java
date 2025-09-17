package com.trever.backend.vehicle.service;


import com.trever.backend.common.config.firebase.FirebaseStorageService;
import com.trever.backend.common.exception.BadRequestException;
import com.trever.backend.vehicle.dto.VehiclePhotoDto;
import com.trever.backend.vehicle.entity.Vehicle;
import com.trever.backend.vehicle.entity.VehiclePhoto;
import com.trever.backend.vehicle.repository.VehiclePhotoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiclePhotoService {

    private final VehiclePhotoRepository vehiclePhotoRepository;
    private final FirebaseStorageService firebaseStorageService;
    
    /**
     * 차량 사진 업로드 및 저장
     */
    @Transactional
    public List<VehiclePhoto> uploadAndSaveVehiclePhotos(
            Vehicle vehicle,
            List<MultipartFile> photoFiles,
            List<Integer> photoOrders) {
        
        if (photoFiles == null || photoFiles.isEmpty()) {
            return List.of();
        }
        
        if (photoFiles.size() > 5) {
            throw new BadRequestException("차량 사진은 최대 5개까지 등록 가능합니다.");
        }
        
        List<VehiclePhoto> savedPhotos = new ArrayList<>();
        
        for (int i = 0; i < photoFiles.size(); i++) {
            MultipartFile file = photoFiles.get(i);
            
            try {
                // Firebase Storage에 이미지 업로드
                String photoUrl = firebaseStorageService.uploadImage(file, "vehicles");
                
                // 순서 정보 결정
                Integer orderIndex = (photoOrders != null && i < photoOrders.size()) 
                    ? photoOrders.get(i) : i;
                
                // VehiclePhoto 엔티티 생성 및 저장
                VehiclePhoto vehiclePhoto = VehiclePhoto.builder()
                        .vehicle(vehicle)
                        .photoUrl(photoUrl)
                        .orderIndex(orderIndex)
                        .build();
                
                savedPhotos.add(vehiclePhotoRepository.save(vehiclePhoto));
                
                if (orderIndex == 0) {
                    vehicle.setRepresentativePhotoUrl(photoUrl);
                }
            } catch (IOException e) {
                // 에러 발생 시 이미 업로드된 사진 삭제
                savedPhotos.forEach(photo -> firebaseStorageService.deleteImage(photo.getPhotoUrl()));
                vehiclePhotoRepository.deleteAll(savedPhotos);
                
                throw new BadRequestException("사진 업로드 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        return savedPhotos;
    }
    
    /**
     * 차량 ID로 사진 목록 조회
     */
    public List<VehiclePhoto> getVehiclePhotos(Long vehicleId) {
        return vehiclePhotoRepository.findByVehicleIdOrderByOrderIndex(vehicleId);
    }
    
    /**
     * 차량 사진 삭제
     */
    @Transactional
    public void deleteVehiclePhotos(Long vehicleId) {
        List<VehiclePhoto> photos = vehiclePhotoRepository.findByVehicleIdOrderByOrderIndex(vehicleId);
        
        // Firebase Storage에서 이미지 파일 삭제
        for (VehiclePhoto photo : photos) {
            firebaseStorageService.deleteImage(photo.getPhotoUrl());
        }
        
        // 데이터베이스에서 레코드 삭제
        vehiclePhotoRepository.deleteByVehicleId(vehicleId);
    }
}