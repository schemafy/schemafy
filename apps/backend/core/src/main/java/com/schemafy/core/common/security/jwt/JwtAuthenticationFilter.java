package com.schemafy.core.common.security.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            return chain.filter(exchange);
        }

        // JWT 검증을 boundedElastic 스케줄러로 분리 (블로킹 호출 방지)
        return Mono.fromCallable(() -> validateTokenAndGetAuth(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(authResult -> {
                    if (authResult.isValid()) {
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authResult.getAuthentication()));
                    } else {
                        return handleJwtError(exchange, authResult.getErrorType(), authResult.getErrorMessage());
                    }
                })
                .onErrorResume(e -> handleJwtError(exchange, "INVALID_TOKEN", "토큰 검증 중 오류가 발생했습니다."));
    }

    private AuthenticationResult validateTokenAndGetAuth(String token) {
        try {
            String userId = jwtProvider.extractUserId(token);
            String tokenType = jwtProvider.getTokenType(token);

            if (!"ACCESS".equals(tokenType)) {
                return AuthenticationResult.error("INVALID_TOKEN_TYPE", "액세스 토큰이 아닙니다.");
            }

            if (!jwtProvider.validateToken(token, userId)) {
                return AuthenticationResult.error("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

            return AuthenticationResult.success(authentication);

        } catch (ExpiredJwtException e) {
            return AuthenticationResult.error("EXPIRED_TOKEN", "만료된 토큰입니다.");
        } catch (JwtException e) {
            return AuthenticationResult.error("INVALID_TOKEN", "위조되거나 손상된 토큰입니다.");
        } catch (Exception e) {
            return AuthenticationResult.error("INVALID_TOKEN", "토큰 검증 중 오류가 발생했습니다.");
        }
    }

    private Mono<Void> handleJwtError(ServerWebExchange exchange, String errorType, String errorMessage) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", errorMessage);
        errorResponse.put("errorType", errorType);
        errorResponse.put("path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private static class AuthenticationResult {
        private final boolean valid;
        private final UsernamePasswordAuthenticationToken authentication;
        private final String errorType;
        private final String errorMessage;

        private AuthenticationResult(boolean valid, UsernamePasswordAuthenticationToken authentication,
                                     String errorType, String errorMessage) {
            this.valid = valid;
            this.authentication = authentication;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        static AuthenticationResult success(UsernamePasswordAuthenticationToken authentication) {
            return new AuthenticationResult(true, authentication, null, null);
        }

        static AuthenticationResult error(String errorType, String errorMessage) {
            return new AuthenticationResult(false, null, errorType, errorMessage);
        }

        boolean isValid() {
            return valid;
        }

        UsernamePasswordAuthenticationToken getAuthentication() {
            return authentication;
        }

        String getErrorType() {
            return errorType;
        }

        String getErrorMessage() {
            return errorMessage;
        }
    }
}