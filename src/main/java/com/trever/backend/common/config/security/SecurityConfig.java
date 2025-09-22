package com.trever.backend.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trever.backend.api.jwt.JwtFilter;
import com.trever.backend.api.jwt.JwtProvider;
import com.trever.backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper; // 추가

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/api-doc"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/users/signup",
                                "/api/v1/users/reissue",
                                "/api/v1/users/login",
                                "/api/v1/users/auth/google/login"
                        ).permitAll()
                        .requestMatchers(
                                "/api/cars/**",
                                "/api/vehicles/**",
                                "/api/vehicle-options/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            if (request.getRequestURI().contains("/api/auctions/bids")) {
                                response.setStatus(HttpStatus.BAD_REQUEST.value());

                                ApiResponse<?> errorResponse = ApiResponse.builder()
                                        .status(400)
                                        .message("입찰을 위한 인증이 필요합니다: " + authException.getMessage())
                                        .build();

                                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                            } else {
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());

                                ApiResponse<?> errorResponse = ApiResponse.builder()
                                        .status(401)
                                        .message("인증이 필요합니다")
                                        .build();

                                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            if (request.getRequestURI().contains("/api/auctions/bids")) {
                                response.setStatus(HttpStatus.BAD_REQUEST.value());

                                ApiResponse<?> errorResponse = ApiResponse.builder()
                                        .status(400)
                                        .message("접근 권한이 없습니다")
                                        .build();

                                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                            } else {
                                response.setStatus(HttpStatus.FORBIDDEN.value());

                                ApiResponse<?> errorResponse = ApiResponse.builder()
                                        .status(403)
                                        .message("접근 권한이 없습니다")
                                        .build();

                                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:8080",
                "https://trever.store",
                "https://www.trever.store"
        ));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
