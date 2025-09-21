package com.trever.backend.basiccar.repository;

import com.trever.backend.basiccar.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    // 1. 카테고리별 제조사 목록
    @Query("SELECT DISTINCT c.manufacturer FROM CarModel c WHERE c.category = :category")
    List<String> findDistinctManufacturerByCategory(String category);

    // 2. 카테고리+제조사별 차명 목록
    @Query("SELECT DISTINCT c.carName FROM CarModel c WHERE c.category = :category AND c.manufacturer = :manufacturer")
    List<String> findDistinctCarNameByCategoryAndManufacturer(String category, String manufacturer);

    // 3. 카테고리+제조사+차명별 모델 목록
    @Query("SELECT DISTINCT c.modelName FROM CarModel c WHERE c.category = :category AND c.manufacturer = :manufacturer AND c.carName = :carName")
    List<String> findDistinctModelNameByCategoryAndManufacturerAndCarName(String category, String manufacturer, String carName);

    // 4. 카테고리+제조사+차명+모델별 연식 목록
    @Query("SELECT DISTINCT c.carYear FROM CarModel c WHERE c.category = :category AND c.manufacturer = :manufacturer AND c.carName = :carName AND c.modelName = :modelName")
    List<Integer> findDistinctYearByCategoryAndManufacturerAndCarNameAndModelName(String category, String manufacturer, String carName, String modelName);

    @Query("SELECT DISTINCT c.manufacturer FROM CarModel c ORDER BY c.manufacturer")
    List<String> findDistinctManufacturers();

    @Query("SELECT DISTINCT c.carName FROM CarModel c WHERE c.manufacturer = :manufacturer ORDER BY c.carName")
    List<String> findDistinctCarNamesByManufacturer(@Param("manufacturer") String manufacturer);

    @Query("SELECT DISTINCT c.modelName FROM CarModel c WHERE c.manufacturer = :manufacturer AND c.carName = :carName ORDER BY c.modelName")
    List<String> findDistinctCarModelsByManufacturerAndCarName(
            @Param("manufacturer") String manufacturer,
            @Param("carName") String carName
    );

    // 국산 제조사 조회 (category 필드가 있는 경우)
    @Query("SELECT DISTINCT c.manufacturer FROM CarModel c WHERE c.category = '국산' ORDER BY c.manufacturer")
    List<String> findDistinctDomesticManufacturers();

    // 수입 제조사 조회 (category 필드가 있는 경우)
    @Query("SELECT DISTINCT c.manufacturer FROM CarModel c WHERE c.category = '수입' ORDER BY c.manufacturer")
    List<String> findDistinctImportedManufacturers();

}

