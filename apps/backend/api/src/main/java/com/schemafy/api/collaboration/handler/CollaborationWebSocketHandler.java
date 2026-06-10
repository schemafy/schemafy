package com.schemafy.api.collaboration.handler;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

import com.schemafy.api.collaboration.security.ProjectAccessValidator;
import com.schemafy.api.collaboration.security.WebSocketAuthInfo;
import com.schemafy.api.collaboration.service.CollaborationDirectMessageSender;
import com.schemafy.api.collaboration.service.CollaborationService;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.collaboration.service.presence.CollaborationPresenceProperties;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.api.common.security.principal.AuthenticatedUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationWebSocketHandler implements WebSocketHandler {

  private final CollaborationDirectMessageSender directMessageSender;
  private final CollaborationService collaborationService;
  private final SessionRegistry sessionRegistry;
  private final ProjectAccessValidator projectAccessValidator;
  private final CollaborationPresenceProperties presenceProperties;

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    URI uri = session.getHandshakeInfo().getUri();

    Optional<String> projectIdOpt = extractProjectId(uri);
    if (projectIdOpt.isEmpty()) {
      return handleInvalidProjectId(session);
    }

    return session.getHandshakeInfo()
        .getPrincipal()
        .ofType(Authentication.class)
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty())
        .flatMap(authenticationOpt -> authenticationOpt
            .map(authentication -> handleAuthentication(session, authentication,
                projectIdOpt.get()))
            .orElseGet(() -> handleUnauthenticated(session)));
  }

  private Mono<Void> handleAuthentication(WebSocketSession session,
      Authentication authentication, String projectId) {
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof AuthenticatedUser user)) {
      return handleUnauthenticated(session);
    }

    String userId = user.userId();
    String userName = user.userName();
    if (userId == null || userId.isBlank()) {
      return handleUnauthenticated(session);
    }
    if (userName == null || userName.isBlank()) {
      return handleUnauthenticated(session);
    }

    WebSocketAuthInfo authInfo = WebSocketAuthInfo.of(userId, userName);
    return validateProjectAccess(session, authInfo, projectId);
  }

  private Optional<String> extractProjectId(URI uri) {
    try {
      MultiValueMap<String, String> params = UriComponentsBuilder
          .fromUri(uri)
          .build()
          .getQueryParams();
      String projectId = params.getFirst("projectId");
      if (projectId == null || projectId.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(projectId);
    } catch (Exception e) {
      log.warn(
          "[CollaborationWebSocketHandler] ProjectId extraction failed: {}",
          e.getMessage());
      return Optional.empty();
    }
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
    String sessionId = session.getId();
    String userId = authInfo.getUserId();
    String userName = authInfo.getUserName();

    log.info(
        "[CollaborationWebSocketHandler] WebSocket connected: sessionId={}, projectId={}, userId={}",
        sessionId, projectId, userId);

    SessionEntry entry = sessionRegistry.addSession(projectId, sessionId,
        session, authInfo);

    Mono<Void> notifyJoin = Mono.defer(() -> collaborationService
        .notifyJoin(projectId, sessionId, userId, userName))
        .doOnError(e -> log.warn(
            "[CollaborationWebSocketHandler] Failed to notify join: sessionId={}, error={}",
            sessionId, e.getMessage()))
        .onErrorResume(e -> Mono.empty());

    Mono<Void> closeSignal = session.closeStatus().then();

    Mono<Void> inbound = session.receive()
        .flatMap(message -> handleInboundMessage(projectId, sessionId,
            message))
        .doOnError(error -> log.error(
            "[CollaborationWebSocketHandler] Inbound error: sessionId={}",
            sessionId, error))
        .then();

    Flux<WebSocketMessage> presencePings = Flux
        .interval(presenceProperties.getHeartbeatInterval())
        .takeUntilOther(closeSignal)
        .map(tick -> session.pingMessage(bufferFactory -> bufferFactory.wrap(new byte[0])))
        .doOnError(error -> log.error(
            "[CollaborationWebSocketHandler] Presence ping error: sessionId={}",
            sessionId, error));

    Mono<Void> outbound = session
        .send(Flux.merge(entry.outboundFlux(), presencePings))
        .doOnError(error -> log.error(
            "[CollaborationWebSocketHandler] Outbound error: sessionId={}",
            sessionId, error));

    Mono<Void> cursorPipeline = entry.sampledCursorFlux()
        .takeUntilOther(closeSignal)
        .flatMap(cursor -> collaborationService
            .publishCursorToRedis(projectId, sessionId, cursor))
        .doOnError(error -> log.error(
            "[CollaborationWebSocketHandler] Cursor pipeline error: sessionId={}",
            sessionId, error))
        .then();

    return collaborationService
        .registerSession(projectId, sessionId, userId, userName)
        .flatMap(participants -> directMessageSender
            .sendSessionReady(entry, sessionId, participants)
            .doOnError(e -> log.warn(
                "[CollaborationWebSocketHandler] Failed to send session ready: sessionId={}, error={}",
                sessionId, e.getMessage()))
            .then(notifyJoin))
        .then(Mono.when(inbound, outbound, cursorPipeline))
        .doFinally(signalType -> {
          log.info(
              "[CollaborationWebSocketHandler] WebSocket disconnected: sessionId={}, signal={}",
              sessionId, signalType);
          collaborationService.removeSession(projectId, sessionId)
              .timeout(Duration.ofSeconds(5))
              .doOnError(e -> log.warn(
                  "[CollaborationWebSocketHandler] Failed to remove session: sessionId={}, error={}",
                  sessionId, e.getMessage()))
              .onErrorResume(e -> Mono.empty())
              .subscribe();
        });
  }

  private Mono<Void> handleInboundMessage(String projectId,
      String sessionId, WebSocketMessage message) {
    if (message.getType() == WebSocketMessage.Type.PONG) {
      return collaborationService.refreshPresence(projectId, sessionId)
          .doOnError(e -> log.warn(
              "[CollaborationWebSocketHandler] Failed to refresh presence from pong: sessionId={}, error={}",
              sessionId, e.getMessage()))
          .onErrorResume(e -> Mono.empty());
    }

    if (message.getType() != WebSocketMessage.Type.TEXT) {
      return Mono.empty();
    }

    return collaborationService
        .handleMessage(projectId, sessionId, message.getPayloadAsText())
        .doOnError(e -> log.warn(
            "[CollaborationWebSocketHandler] Failed to handle message: sessionId={}, error={}",
            sessionId, e.getMessage()))
        .onErrorResume(e -> Mono.empty());
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
