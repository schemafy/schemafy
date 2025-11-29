package com.schemafy.core.collaboration.security;

import java.net.URI;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.schemafy.core.common.security.jwt.JwtProvider;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketAuthenticator {

    private static final String TOKEN_PARAM = "token";
    private static final String CLAIM_NAME = "name";

    private final JwtProvider jwtProvider;

    public Optional<WebSocketAuthInfo> authenticate(URI uri) {
        // /ws/collaboration/{projectId}?token={accessToken}
        String token = extractToken(uri);

        if (token == null || token.isBlank()) {
            log.warn(
                    "[WebSocketAuthenticator] WebSocket authentication failed: No token provided.");
            return Optional.empty();
        }

        try {
            String userId = jwtProvider.extractUserId(token);
            String tokenType = jwtProvider.getTokenType(token);

            if (!JwtProvider.ACCESS_TOKEN.equals(tokenType)) {
                log.warn(
                        "[WebSocketAuthenticator] WebSocket authentication failed: Invalid token type - {}",
                        tokenType);
                return Optional.empty();
            }

            if (!jwtProvider.validateToken(token, userId)) {
                log.warn(
                        "[WebSocketAuthenticator] WebSocket authentication failed: Token validation failed");
                return Optional.empty();
            }

            Optional<String> userNameOpt = extractUserName(token);
            if (userNameOpt.isEmpty()) {
                log.warn(
                        "[WebSocketAuthenticator] WebSocket authentication failed: Missing user name (claim)");
                return Optional.empty();
            }

            String userName = userNameOpt.get();
            log.debug(
                    "[WebSocketAuthenticator] WebSocket authentication succeeded: userId={}",
                    userId);
            return Optional.of(WebSocketAuthInfo.of(userId, userName));

        } catch (ExpiredJwtException e) {
            log.warn(
                    "[WebSocketAuthenticator] WebSocket authentication failed: Token expired");
            return Optional.empty();
        } catch (JwtException e) {
            log.warn(
                    "[WebSocketAuthenticator] WebSocket authentication failed: Invalid token format - {}",
                    e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error(
                    "[WebSocketAuthenticator] WebSocket authentication failed: Unexpected error",
                    e);
            return Optional.empty();
        }
    }

    private String extractToken(URI uri) {
        try {
            MultiValueMap<String, String> params = UriComponentsBuilder
                    .fromUri(uri)
                    .build()
                    .getQueryParams();
            return params.getFirst(TOKEN_PARAM);
        } catch (Exception e) {
            log.warn("[WebSocketAuthenticator] Token extraction failed: {}",
                    e.getMessage());
            return null;
        }
    }

    private Optional<String> extractUserName(String token) {
        try {
            String name = jwtProvider.extractClaim(token,
                    claims -> claims.get(CLAIM_NAME, String.class));
            return Optional.ofNullable(name);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
