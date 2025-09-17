package com.trever.backend.common.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Value("${firebase.config.path}")
    private String configPath;

    @PostConstruct
    public void initialize() {
        try {
            // 만약 FirebaseApp.getInstance()를 호출했을 때 예외가 발생하지 않으면 이미 초기화된 것
            try {
                FirebaseApp.getInstance();
                log.info("Firebase 앱이 이미 초기화되었습니다.");
                return;
            } catch (IllegalStateException e) {
                // 인스턴스가 없으면 초기화 계속 진행
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ClassPathResource(configPath).getInputStream()))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase 앱이 초기화되었습니다.");
        } catch (IOException e) {
            log.error("Firebase 초기화 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    @Bean
    public FirebaseDatabase firebaseDatabase() {
        return FirebaseDatabase.getInstance();
    }
}
