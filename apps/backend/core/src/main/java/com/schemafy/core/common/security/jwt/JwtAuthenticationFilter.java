package com.schemafy.core.common.security.jwt;

import java.util.Collections;

import jakarta.validation.constraints.NotNull;

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

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.schemafy.core.common.exception.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final WebExchangeErrorWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(@NotNull ServerWebExchange exchange,
            @NonNull WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            return chain.filter(exchange);
        }

        // JWT 검증을 boundedElastic 스케줄러로 분리 (블로킹 호출 방지)
        return Mono.fromCallable(() -> validateTokenAndGetAuth(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    if (result.valid()) {
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder
                                        .withAuthentication(
                                                result.authentication()));
                    } else {
                        return handleJwtError(exchange,
                                result.errorType(),
                                result.errorMessage(),
                                result.status());
                    }
                })
                .onErrorResume(e -> handleUnexpectedError(exchange));
    }

    private AuthenticationResult validateTokenAndGetAuth(String token) {
        try {
            String userId = jwtProvider.extractUserId(token);
            String tokenType = jwtProvider.getTokenType(token);

            if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
                return AuthenticationResult
                        .error(ErrorCode.INVALID_ACCESS_TOKEN_TYPE);
            }

            if (!jwtProvider.validateToken(token, userId)) {
                return AuthenticationResult.error(ErrorCode.INVALID_TOKEN);
            }

            UserDetails userDetails = new User(userId, "",
                    Collections.emptyList());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            return AuthenticationResult.success(authentication);

        } catch (ExpiredJwtException e) {
            return AuthenticationResult.error(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            return AuthenticationResult.error(ErrorCode.MALFORMED_TOKEN);
        } catch (Exception e) {
            return AuthenticationResult.error(ErrorCode.TOKEN_VALIDATION_ERROR);
        }
    }

    private Mono<Void> handleJwtError(ServerWebExchange exchange,
            String errorCode, String errorMessage, HttpStatus status) {
        return errorResponseWriter.writeErrorResponse(
                exchange,
                status,
                errorCode,
                errorMessage);
    }

    private Mono<Void> handleJwtError(ServerWebExchange exchange,
            ErrorCode errorCode) {
        return errorResponseWriter.writeErrorResponse(
                exchange,
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage());
    }

    private Mono<Void> handleUnexpectedError(ServerWebExchange exchange) {
        return handleJwtError(exchange, ErrorCode.TOKEN_VALIDATION_ERROR);
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(bearerToken)
                && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private record AuthenticationResult(
            boolean valid,
            UsernamePasswordAuthenticationToken authentication,
            HttpStatus status,
            String errorType,
            String errorMessage) {

        static AuthenticationResult success(
                UsernamePasswordAuthenticationToken authentication) {
            return new AuthenticationResult(true, authentication, null, null,
                    null);
        }

        static AuthenticationResult error(ErrorCode errorCode) {
            return new AuthenticationResult(false, null, errorCode.getStatus(),
                    errorCode.getCode(), errorCode.getMessage());
        }
    }
}
