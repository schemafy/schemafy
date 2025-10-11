package com.schemafy.core.common.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration.ofDays(7);

    private final JwtProvider jwtProvider;

    public <T> ResponseEntity<T> issueTokens(String userId, T body) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();

        String accessToken = jwtProvider.generateAccessToken(userId, claims, now);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // Refresh Token을 HttpOnly Cookie로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS에서만 전송
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite("Strict") // CSRF 방어
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken);
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }
}
