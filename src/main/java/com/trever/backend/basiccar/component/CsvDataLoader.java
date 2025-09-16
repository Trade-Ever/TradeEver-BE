//package com.trever.backend.basiccar.component;
//
//import com.trever.backend.basiccar.entity.CarModel;
//import com.trever.backend.basiccar.repository.CarModelRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//
//@Component
//public class CsvDataLoader implements CommandLineRunner {
//
//    private final CarModelRepository repository;
//
//    public CsvDataLoader(CarModelRepository repository) {
//        this.repository = repository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        ClassPathResource resource = new ClassPathResource("수입차종정보.csv");
//
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
//            String line;
//            boolean firstLine = true;
//
//            while ((line = br.readLine()) != null) {
//                // 헤더는 건너뛰기
//                if (firstLine) {
//                    firstLine = false;
//                    continue;
//                }
//
//                String[] tokens = line.split(",");
//                if (tokens.length < 5) continue;
//
//                CarModel car = new CarModel();
//                car.setCategory(tokens[0].trim());
//                car.setManufacturer(tokens[1].trim());
//                car.setCarName(tokens[2].trim());
//                car.setModelName(tokens[3].trim());
//                car.setCarYear(Integer.parseInt(tokens[4].trim()));
//
//                repository.save(car);
//            }
//
//            System.out.println("CSV 데이터가 DB에 성공적으로 입력되었습니다!");
//        }
//    }
//}
//
