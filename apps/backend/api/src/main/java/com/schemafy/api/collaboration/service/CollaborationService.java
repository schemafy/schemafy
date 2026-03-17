package com.schemafy.api.collaboration.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.schemafy.api.collaboration.dto.BroadcastMessage;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.CursorPosition;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.CursorEvent;
import com.schemafy.api.collaboration.service.handler.InboundMessageHandler;
import com.schemafy.api.collaboration.service.handler.MessageContext;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class CollaborationService {

  private static final double CURSOR_POSITION_EPS = 0.5;

  private final SessionRegistry sessionRegistry;
  private final CollaborationEventPublisher eventPublisher;
  private final CollaborationPayloadSerializer payloadSerializer;
  private final JsonCodec jsonCodec;
  private final Map<CollaborationEventType, InboundMessageHandler> handlers;
  private final Map<String, CursorPosition> cursorDedupeCache = new ConcurrentHashMap<>();

  public CollaborationService(
      SessionRegistry sessionRegistry,
      CollaborationEventPublisher eventPublisher,
      CollaborationPayloadSerializer payloadSerializer,
      JsonCodec jsonCodec,
      List<InboundMessageHandler> handlerList) {
    this.sessionRegistry = sessionRegistry;
    this.eventPublisher = eventPublisher;
    this.payloadSerializer = payloadSerializer;
    this.jsonCodec = jsonCodec;
    this.handlers = handlerList.stream()
        .collect(Collectors.toMap(
            InboundMessageHandler::supportedType,
            Function.identity()));

    log.info("[CollaborationService] Registered {} message handlers: {}",
        handlers.size(), handlers.keySet());
  }

  public Mono<Void> publishCursorToRedis(String projectId, String sessionId,
      CursorPosition cursor) {
    CursorPosition previousCursor = cursorDedupeCache.get(sessionId);
    if (isDuplicateCursor(previousCursor, cursor)) {
      return Mono.empty();
    }
    cursorDedupeCache.put(sessionId, cursor);

    SessionEntry entry = sessionRegistry.getSessionEntry(projectId, sessionId)
        .orElse(null);

    if (entry == null) {
      return Mono.empty();
    }

    String userId = entry.authInfo().getUserId();
    String userName = entry.authInfo().getUserName();
    CursorEvent.UserInfo userInfo = new CursorEvent.UserInfo(userId, userName);

    return eventPublisher.publish(projectId,
        CollaborationOutboundFactory.cursor(sessionId, userInfo, cursor));
  }

  public Mono<Void> removeSession(String projectId, String sessionId) {
    cursorDedupeCache.remove(sessionId);

    SessionEntry entry = sessionRegistry
        .getSessionEntry(projectId, sessionId)
        .orElse(null);
    if (entry == null) {
      log.warn(
          "[CollaborationService] Session not found for removal: sessionId={}",
          sessionId);
      return Mono.fromRunnable(
          () -> sessionRegistry.removeSession(projectId, sessionId))
          .then();
    }

    String userId = entry.authInfo().getUserId();
    String userName = entry.authInfo().getUserName();

    return Mono
        .fromRunnable(() -> sessionRegistry.removeSession(projectId,
            sessionId))
        .then(eventPublisher.publish(projectId,
            CollaborationOutboundFactory.leave(sessionId, userId,
                userName)));
  }

  public Mono<Void> notifyJoin(String projectId, String sessionId,
      String userId, String userName) {
    return eventPublisher.publish(projectId,
        CollaborationOutboundFactory.join(sessionId, userId, userName));
  }

  /** handle inbound message */
  public Mono<Void> handleMessage(String projectId, String sessionId,
      String payload) {
    return deserializeFromJson(payload, CollaborationInbound.class)
        .flatMap(message -> {
          CollaborationEventType type = message.type();
          if (type == null) {
            log.warn(
                "[CollaborationService] No message type: sessionId={}",
                sessionId);
            return Mono.empty();
          }

          InboundMessageHandler handler = handlers.get(type);
          if (handler == null) {
            log.warn(
                "[CollaborationService] No handler for message type: type={}, sessionId={}",
                type, sessionId);
            return Mono.empty();
          }

          MessageContext context = MessageContext.of(projectId,
              sessionId);
          return handler.handle(context, message);
        })
        .onErrorResume(e -> {
          log.warn(
              "[CollaborationService] Message handling failed: {}",
              e.getMessage());
          return Mono.empty();
        });
  }

  /** handle redis message */
  public Mono<Void> handleRedisMessage(String projectId, String message) {
    return deserializeFromJson(message, CollaborationOutbound.class)
        .flatMap(event -> {
          // SESSION_READY는 연결된 세션에 직접 전송된다.
          // 혹시 Redis 경로로 유입되더라도 브로드캐스트하지 않도록 방어적으로 무시한다.
          if (event.type() == CollaborationEventType.SESSION_READY) {
            log.warn(
                "[CollaborationService] Ignoring SESSION_READY from Redis: projectId={}",
                projectId);
            return Mono.empty();
          }

          return payloadSerializer.serializeForBroadcast(event)
              .doOnNext(json -> broadcastEvent(
                  projectId,
                  event.sessionId(),
                  event.type(),
                  json))
              .then();
        });
  }

  private void broadcastEvent(String projectId, String excludeSessionId,
      CollaborationEventType eventType, String json) {
    if (eventType == null) {
      log.warn(
          "[CollaborationService] Event type is null, using best effort");
      sessionRegistry.broadcast(
          BroadcastMessage.of(projectId, excludeSessionId, json));
      return;
    }

    String exclude = eventType.shouldIncludeSender() ? null
        : excludeSessionId;
    sessionRegistry
        .broadcast(BroadcastMessage.of(projectId, exclude, json));
  }

  private <T> Mono<T> deserializeFromJson(String json, Class<T> clazz) {
    return Mono.fromCallable(() -> jsonCodec.parse(json, clazz))
        .onErrorMap(IllegalArgumentException.class,
            e -> new RuntimeException("[CollaborationService] failed to deserialize JSON",
                e));
  }

  private boolean isDuplicateCursor(CursorPosition first,
      CursorPosition second) {
    if (first == null || second == null) {
      return false;
    }

    return Math.abs(first.x() - second.x()) < CURSOR_POSITION_EPS
        && Math.abs(first.y() - second.y()) < CURSOR_POSITION_EPS;
  }

}
