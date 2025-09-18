package com.trever.backend.api.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtProvider {

    private static final String AUTHORITIES_KEY = "auth"; // 권한 정보를 저장하는 키
    private final Key key; // JWT 서명에 사용할 비밀키

    // 비밀키를 기반으로 키 객체 초기화
    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        // 암호화
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // 액세스 토큰 생성 메서드
    public String generateAccessToken(Authentication authentication) {
        // 인증 객체에서 권한 정보 추출
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 현재 시간과 토큰 만료 시간 계산
        Date accessTokenExpiresIn = new Date(System.currentTimeMillis() + 30 * 60 * 1000);  // 30분

        // Access Token 생성
        return Jwts.builder()
                        .setSubject(authentication.getName()) // 사용자명 설정, 이메일이 들어 있음
                        .claim(AUTHORITIES_KEY, roles)
                        .setExpiration(accessTokenExpiresIn)  // 만료 시간 설정
                        .signWith(key, SignatureAlgorithm.HS512) // 서명 방식 설정
                        .compact();
    }

    // 리프레시 토큰 생성 메서드
    public String generateRefreshToken(String email) {
        Date refreshTokenExpiresIn = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L); // 7일

        return Jwts.builder()
                .setSubject(email)
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 토큰에서 Authentication 추출
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        // 권한 정보 추출 (auth claim 없으면 ROLE_USER 기본 부여)
        List<String> roles = claims.get(AUTHORITIES_KEY, List.class);
        if (roles == null || roles.isEmpty()) {
            roles = List.of("ROLE_USER");
        }

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 인증 객체 생성 후 반환
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token);
    }

    // 토큰의 Claims 추출
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
