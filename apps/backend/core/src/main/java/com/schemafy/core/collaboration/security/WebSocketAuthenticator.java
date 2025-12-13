package com.schemafy.core.collaboration.security;

import java.net.URI;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.security.jwt.JwtProvider;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class WebSocketAuthenticator {

    private static final String TOKEN_PARAM = "token";
    private static final String PROJECT_ID_PARAM = "projectId";
    private static final String CLAIM_NAME = "name";

    private final JwtProvider jwtProvider;

    public Optional<WebSocketAuthInfo> authenticate(URI uri) {
        // /ws/collaboration?projectId={projectId}&token={accessToken}
        Optional<String> tokenOpt = extractToken(uri);

        if (tokenOpt.isEmpty()) {
            log.warn(
                    "[WebSocketAuthenticator] WebSocket authentication failed: No token provided.");
            return Optional.empty();
        }

        String token = tokenOpt.get();

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

    private Optional<String> extractToken(URI uri) {
        try {
            MultiValueMap<String, String> params = UriComponentsBuilder
                    .fromUri(uri)
                    .build()
                    .getQueryParams();
            String token = params.getFirst(TOKEN_PARAM);
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(token);
        } catch (Exception e) {
            log.warn("[WebSocketAuthenticator] Token extraction failed: {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractProjectId(URI uri) {
        try {
            MultiValueMap<String, String> params = UriComponentsBuilder
                    .fromUri(uri)
                    .build()
                    .getQueryParams();
            String projectId = params.getFirst(PROJECT_ID_PARAM);
            if (projectId == null || projectId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(projectId);
        } catch (Exception e) {
            log.warn("[WebSocketAuthenticator] ProjectId extraction failed: {}",
                    e.getMessage());
            return Optional.empty();
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
