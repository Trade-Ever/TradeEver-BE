package com.trever.backend.basiccar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;      // 수입 / 국산
    private String manufacturer;  // BMW, 현대 등
    private String carName;       // 1시리즈, 그랜저 등
    private String modelName;     // 1시리즈, 1시리즈(2세대)
    private Integer carYear; //연식
}
