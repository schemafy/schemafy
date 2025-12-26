package com.schemafy.core.collaboration.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.BroadcastMessage;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.dto.event.CollaborationInbound;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.collaboration.service.handler.InboundMessageHandler;
import com.schemafy.core.collaboration.service.handler.MessageContext;
import com.schemafy.core.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class CollaborationService {

    private static final double CURSOR_POSITION_EPS = 0.5;

    private final SessionRegistry sessionRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<CollaborationEventType, InboundMessageHandler> handlers;
    private final Map<String, CursorPosition> cursorDedupeCache = new ConcurrentHashMap<>();

    public CollaborationService(
            SessionRegistry sessionRegistry,
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            List<InboundMessageHandler> handlerList) {
        this.sessionRegistry = sessionRegistry;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        InboundMessageHandler::supportedType,
                        Function.identity()));

        log.info("[CollaborationService] Registered {} message handlers: {}",
                handlers.size(), handlers.keySet());
    }

    public Mono<Void> publishCursorToRedis(String projectId, String sessionId,
            CursorPosition cursor) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        CursorPosition previousCursor = cursorDedupeCache.get(sessionId);
        if (isDuplicateCursor(previousCursor, cursor)) {
            return Mono.empty();
        }
        cursorDedupeCache.put(sessionId, cursor);

        return serializeToJson(CollaborationOutboundFactory.cursor(sessionId, cursor))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .doOnError(e -> log.warn(
                        "[CollaborationService] Failed to publish cursor: sessionId={}, error={}",
                        sessionId, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    public Mono<Void> removeSession(String projectId, String sessionId) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;
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
                .then(serializeToJson(CollaborationOutboundFactory.leave(sessionId,
                        userId, userName)))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .then();
    }

    public Mono<Void> notifyJoin(String projectId, String sessionId,
            String userId, String userName) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        return serializeToJson(
                CollaborationOutboundFactory.join(sessionId, userId, userName))
                .flatMap(eventJson -> redisTemplate.convertAndSend(
                        channelName,
                        eventJson))
                .then();
    }

    /**
     * handle inbound message
     */
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
                    log.warn("[CollaborationService] Message handling failed: {}",
                            e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * handle redis message
     */
    public Mono<Void> handleRedisMessage(String projectId, String message) {
        return deserializeFromJson(message, CollaborationOutbound.class)
                .flatMap(event -> serializeToJson(event.withoutSessionId())
                        .doOnNext(json -> broadcastEvent(
                                projectId,
                                event.sessionId(),
                                event.type(),
                                json))
                        .then());
    }

    private void broadcastEvent(String projectId, String excludeSessionId,
            CollaborationEventType eventType, String json) {
        if (eventType == null) {
            log.warn("[CollaborationService] Event type is null, using best effort");
            sessionRegistry.broadcast(
                    BroadcastMessage.of(projectId, excludeSessionId, json));
            return;
        }

        String exclude = eventType.shouldIncludeSender() ? null : excludeSessionId;
        sessionRegistry.broadcast(BroadcastMessage.of(projectId, exclude, json));
    }

    private <T> Mono<String> serializeToJson(T object) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(object))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[CollaborationService] failed to serialize JSON",
                                e));
    }

    private <T> Mono<T> deserializeFromJson(String json, Class<T> clazz) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, clazz))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[CollaborationService] failed to deserialize JSON",
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

