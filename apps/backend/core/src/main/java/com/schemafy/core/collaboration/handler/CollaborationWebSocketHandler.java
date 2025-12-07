package com.schemafy.core.collaboration.handler;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.schemafy.core.collaboration.security.ProjectAccessValidator;
import com.schemafy.core.collaboration.security.WebSocketAuthInfo;
import com.schemafy.core.collaboration.security.WebSocketAuthenticator;
import com.schemafy.core.collaboration.service.PresenceService;
import com.schemafy.core.collaboration.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CollaborationWebSocketHandler implements WebSocketHandler {

    private final PresenceService presenceService;
    private final SessionService sessionService;
    private final WebSocketAuthenticator authenticator;
    private final ProjectAccessValidator projectAccessValidator;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();

        Optional<String> projectIdOpt = authenticator.extractProjectId(uri);
        if (projectIdOpt.isEmpty()) {
            return handleInvalidProjectId(session);
        }

        return authenticator.authenticate(uri)
                .map(authInfo -> validateProjectAccess(session, authInfo,
                        projectIdOpt.get()))
                .orElseGet(() -> handleUnauthenticated(session));
    }

    private Mono<Void> validateProjectAccess(WebSocketSession session,
            WebSocketAuthInfo authInfo, String projectId) {
        String userId = authInfo.getUserId();

        return projectAccessValidator.canAccess(projectId, userId)
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return handleAuthenticated(session, authInfo,
                                projectId);
                    } else {
                        return handleAccessDenied(session, projectId);
                    }
                });
    }

    private Mono<Void> handleAuthenticated(WebSocketSession session,
            WebSocketAuthInfo authInfo, String projectId) {
        String userId = authInfo.getUserId();
        String userName = authInfo.getUserName();

        log.info(
                "[CollaborationWebSocketHandler] WebSocket connected: sessionId={}, projectId={}, userId={}",
                session.getId(), projectId, userId);

        sessionService.addSession(projectId, session.getId(), session,
                authInfo);

        return presenceService
                .notifyJoin(projectId, session.getId(), userId, userName)
                .doOnError(e -> log.warn(
                        "[CollaborationWebSocketHandler] Failed to notify join: sessionId={}, error={}",
                        session.getId(), e.getMessage()))
                .thenMany(session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .sample(Duration.ofMillis(50)))
                .flatMap(payload -> presenceService
                        .handleMessage(projectId, session.getId(), payload)
                        .doOnError(e -> log.warn(
                                "[CollaborationWebSocketHandler] Failed to handle message: sessionId={}, error={}",
                                session.getId(), e.getMessage()))
                        .onErrorResume(e -> Mono.empty()))
                .doOnError(error -> log.error(
                        "[CollaborationWebSocketHandler] WebSocket error: sessionId={}",
                        session.getId(), error))
                .doFinally(signalType -> {
                    log.info(
                            "[CollaborationWebSocketHandler] WebSocket disconnected: sessionId={}, signal={}",
                            session.getId(), signalType);
                })
                .onErrorResume(e -> Mono.empty())
                .then(presenceService.removeSession(projectId, session.getId())
                        .timeout(Duration.ofSeconds(5))
                        .doOnError(e -> log.warn(
                                "[CollaborationWebSocketHandler] Failed to remove session: sessionId={}, error={}",
                                session.getId(), e.getMessage()))
                        .onErrorResume(e -> Mono.empty()));
    }

    private Mono<Void> handleUnauthenticated(WebSocketSession session) {
        log.warn(
                "[CollaborationWebSocketHandler] WebSocket authentication failed: sessionId={}",
                session.getId());
        return session.close(CloseStatus.POLICY_VIOLATION
                .withReason("Authentication required"));
    }

    private Mono<Void> handleAccessDenied(WebSocketSession session,
            String projectId) {
        log.warn(
                "[CollaborationWebSocketHandler] Access denied to project: sessionId={}, projectId={}",
                session.getId(), projectId);
        return session.close(CloseStatus.POLICY_VIOLATION
                .withReason("Access denied to project"));
    }

    private Mono<Void> handleInvalidProjectId(WebSocketSession session) {
        log.warn(
                "[CollaborationWebSocketHandler] Invalid or missing projectId: sessionId={}",
                session.getId());
        return session.close(CloseStatus.BAD_DATA
                .withReason("projectId is required"));
    }

}
