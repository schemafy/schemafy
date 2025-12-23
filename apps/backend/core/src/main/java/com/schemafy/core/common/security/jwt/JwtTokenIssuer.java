package com.schemafy.core.common.security.jwt;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration
            .ofDays(7);
    private static final String CLAIM_NAME = "name";

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public <T> ResponseEntity<T> issueTokens(String userId, String userName,
            T body) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = Map.of(CLAIM_NAME, userName);

        boolean cookieSecure = jwtProperties.getCookie().isSecure();
        String cookieSameSite = jwtProperties.getCookie().getSameSite();

        String accessToken = jwtProvider.generateAccessToken(userId, claims,
                now);
        String refreshToken = jwtProvider.generateRefreshToken(userId);

        // Refresh Token을 HttpOnly Cookie로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .sameSite(cookieSameSite)
                .build();

        ResponseCookie accessTokenCookie = ResponseCookie
                .from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtProvider.getAccessTokenExpiresIn())
                .sameSite(cookieSameSite)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken);
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

}
