package com.schemafy.core.common.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final WebExchangeErrorWriter errorResponseWriter;

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
                .onErrorResume(e -> handleUnexpectedError(exchange));
    }

    private AuthenticationResult validateTokenAndGetAuth(String token) {
        try {
            String userId = jwtProvider.extractUserId(token);
            String tokenType = jwtProvider.getTokenType(token);

            if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
                return AuthenticationResult.error(JwtErrorCode.INVALID_TOKEN_TYPE);
            }

            if (!jwtProvider.validateToken(token, userId)) {
                return AuthenticationResult.error(JwtErrorCode.INVALID_TOKEN);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

            return AuthenticationResult.success(authentication);

        } catch (ExpiredJwtException e) {
            return AuthenticationResult.error(JwtErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            return AuthenticationResult.error(JwtErrorCode.MALFORMED_TOKEN);
        } catch (Exception e) {
            return AuthenticationResult.error(JwtErrorCode.TOKEN_VALIDATION_ERROR);
        }
    }

    private Mono<Void> handleJwtError(ServerWebExchange exchange, String errorCode, String errorMessage) {
        return errorResponseWriter.writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                errorCode,
                errorMessage
        );
    }

    private Mono<Void> handleJwtError(ServerWebExchange exchange, JwtErrorCode jwtErrorCode) {
        return errorResponseWriter.writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                jwtErrorCode.getCode(),
                jwtErrorCode.getMessage()
        );
    }

    private Mono<Void> handleUnexpectedError(ServerWebExchange exchange) {
        return handleJwtError(exchange, JwtErrorCode.TOKEN_VALIDATION_ERROR);
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

        static AuthenticationResult error(JwtErrorCode errorCode) {
            return new AuthenticationResult(false, null, errorCode.getCode(), errorCode.getMessage());
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