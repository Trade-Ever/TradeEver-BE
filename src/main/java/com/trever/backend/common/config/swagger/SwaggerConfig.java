package com.trever.backend.common.config.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

@Configuration
public class SwaggerConfig {

    @Value("${jwt.access.header}")
    private String accessTokenHeader;

    @Value("${jwt.refresh.header}")
    private String refreshTokenHeader;

    @Bean
    public OpenAPI openAPI() {
        // Access Token Bearer 인증 스키마 설정
        SecurityScheme accessTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(accessTokenHeader);

        // Refresh Token Bearer 인증 스키마 설정
        SecurityScheme refreshTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(refreshTokenHeader);

        // SecurityRequirement 설정 - 각 토큰별 인증 요구사항 추가
        SecurityRequirement accessTokenRequirement = new SecurityRequirement().addList(accessTokenHeader);
        SecurityRequirement refreshTokenRequirement = new SecurityRequirement().addList(refreshTokenHeader);

        //로컬용
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버");

        //배포용 1
        Server prodServer1 = new Server()
                .url("https://www.trever.store")
                .description("배포 서버");

        //배포용 2
        Server prodServer2 = new Server()
                .url("http://54.180.107.111:8080")
                .description("배포 서버");

        Components components = new Components()
                .addSchemas("MultipartFile", new Schema<MultipartFile>()
                        .type("string")
                        .format("binary"))
                .addSecuritySchemes(accessTokenHeader, accessTokenScheme)
                .addSecuritySchemes(refreshTokenHeader, refreshTokenScheme);

        return new OpenAPI()
                .info(new Info()
                        .title("Trever")
                        .description("Trever REST API Document")
                        .version("1.0.0"))
                .components(components)
                .addServersItem(localServer)
                .addServersItem(prodServer1)
                .addServersItem(prodServer2)
                .addSecurityItem(accessTokenRequirement)
                .addSecurityItem(refreshTokenRequirement);
    }

    // multipart 파일 업로드를 위한 추가 설정
    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

}