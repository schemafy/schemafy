package com.schemafy.core.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.security.jwt.JwtProperties;
import com.schemafy.core.common.security.jwt.JwtProvider;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    JwtProvider jwtProvider;
    JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties
                .setSecret("test-secret-key-minimum-32-characters-for-hs256");
        jwtProperties.setIssuer("schemafy");
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7 days
        jwtProperties.setAccessTokenExpiration(36000000L); // 1 hour
        jwtProperties.setAudience("schemafy-audience");

        jwtProvider = new JwtProvider(jwtProperties);
    }

    @Test
    @DisplayName("유효한 클레임으로 액세스 토큰을 생성한다")
    void generateValidAccessToken() {
        long now = System.currentTimeMillis();
        String userId = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("email", "test@example.com");

        String token = jwtProvider.generateAccessToken(userId, claims, now);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtProvider.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtProvider.getTokenType(token))
                .isEqualTo(JwtProvider.ACCESS_TOKEN);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰을 생성한다")
    void generateValidRefreshToken() {
        String userId = "user123";

        String token = jwtProvider.generateRefreshToken(userId);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtProvider.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtProvider.getTokenType(token))
                .isEqualTo(JwtProvider.REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰에서 사용자 ID를 올바르게 추출한다")
    void extractUserIdFromToken() {
        long now = System.currentTimeMillis();
        String userId = "user456";
        String token = jwtProvider.generateAccessToken(userId, new HashMap<>(),
                now);

        String extractedUserId = jwtProvider.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("올바른 사용자 ID로 토큰 검증에 성공한다")
    void validateTokenSuccessfully() {
        long now = System.currentTimeMillis();
        String userId = "user789";
        String token = jwtProvider.generateAccessToken(userId, new HashMap<>(),
                now);

        boolean isValid = jwtProvider.validateToken(token, userId);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 토큰 검증에 실패한다")
    void rejectTokenForWrongUserId() {
        long now = System.currentTimeMillis();
        String userId = "user123";
        String wrongUserId = "user456";
        String token = jwtProvider.generateAccessToken(userId, new HashMap<>(),
                now);

        boolean isValid = jwtProvider.validateToken(token, wrongUserId);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료되지 않은 토큰을 감지한다")
    void detectNonExpiredToken() {
        long now = System.currentTimeMillis();
        String userId = "user123";
        String token = jwtProvider.generateAccessToken(userId, new HashMap<>(),
                now);

        boolean isExpired = jwtProvider.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰을 감지한다")
    void detectExpiredToken() {
        long now = System.currentTimeMillis();
        jwtProperties.setAccessTokenExpiration(-1000); // Expired 1 second ago
        JwtProvider expiredJwtProvider = new JwtProvider(jwtProperties);
        String userId = "user123";
        String token = expiredJwtProvider.generateAccessToken(userId,
                new HashMap<>(), now);

        assertThatThrownBy(() -> expiredJwtProvider.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 예외를 발생시킨다")
    void throwExceptionForMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> jwtProvider.extractUserId(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("변조된 토큰은 예외를 발생시킨다")
    void throwExceptionForTamperedToken() {
        long now = System.currentTimeMillis();
        String userId = "user123";
        String token = jwtProvider.generateAccessToken(userId, new HashMap<>(),
                now);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtProvider.extractUserId(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("빈 토큰은 예외를 발생시킨다")
    void throwExceptionForEmptyToken() {
        String emptyToken = "";

        assertThatThrownBy(() -> jwtProvider.extractUserId(emptyToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사용자 ID에 특수 문자가 포함된 토큰을 처리한다")
    void handleSpecialCharactersInUserId() {
        long now = System.currentTimeMillis();
        String userId = "user@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");

        String token = jwtProvider.generateAccessToken(userId, claims, now);
        String extractedUserId = jwtProvider.extractUserId(token);

        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(jwtProvider.validateToken(token, userId)).isTrue();
    }

    @Test
    @DisplayName("여러 클레임을 포함한 토큰을 처리한다")
    void handleMultipleClaims() {
        long now = System.currentTimeMillis();
        String userId = "user123";
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        claims.put("email", "test@example.com");
        claims.put("department", "Engineering");

        String token = jwtProvider.generateAccessToken(userId, claims, now);

        assertThat(token).isNotNull();
        assertThat(jwtProvider.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰은 다른 타입을 가진다")
    void differentTokenTypes() {
        long now = System.currentTimeMillis();
        String userId = "user123";

        String accessToken = jwtProvider.generateAccessToken(userId,
                new HashMap<>(), now);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        assertThat(jwtProvider.getTokenType(accessToken))
                .isEqualTo(JwtProvider.ACCESS_TOKEN);
        assertThat(jwtProvider.getTokenType(refreshToken))
                .isEqualTo(JwtProvider.REFRESH_TOKEN);
    }

}
