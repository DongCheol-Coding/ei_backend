package com.example.ei_backend.security;

import com.example.ei_backend.domain.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;
    private final long accessTokenValidity = 30 * 60 * 1000L;  // 30분
    private final long refreshTokenValidity = 14 * 24 * 60 * 60 * 1000L;  // 2주

    @PostConstruct
    public void init() {
        // 환경변수에서 직접 불러오기
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret == null || envSecret.isBlank()) {
            throw new IllegalStateException("❌ 환경변수 JWT_SECRET가 설정되지 않았습니다.");
        }

        this.secretKey = envSecret;
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("✅ JWT_SECRET Loaded from ENV: {}", secretKey);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles", List.class);
    }


    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⛔ AccessToken 만료됨: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⛔ 유효하지 않은 JWT: {}", e.getMessage());
            return false;
        }
    }

    public String generateAccessToken(String email, List<String> roles) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity)) // 여기 수정
                .signWith(key, SignatureAlgorithm.HS256) // secretKey → key 로도 수정
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getRemainingTime(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }


}

