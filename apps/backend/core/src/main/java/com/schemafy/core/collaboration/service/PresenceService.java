package com.schemafy.core.collaboration.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.ClientMessage;
import com.schemafy.core.collaboration.dto.CursorClientMessage;
import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.dto.PresenceEvent;
import com.schemafy.core.collaboration.dto.PresenceEventFactory;
import com.schemafy.core.collaboration.dto.PresenceEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class PresenceService {

    private final SessionService sessionService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Mono<Void> updateCursor(String projectId, String sessionId,
            CursorPosition cursor) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        String userName = sessionService.getAuthInfo(sessionId)
                .map(auth -> auth.getUserName())
                .orElse("Unknown");

        CursorPosition cursorWithUserName = cursor.withUserName(userName);

        return serializeToJson(
                PresenceEventFactory.cursor(sessionId, cursorWithUserName))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .then();
    }

    public Mono<Void> removeSession(String projectId, String sessionId) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        return Mono
                .fromRunnable(() -> sessionService.removeSession(projectId,
                        sessionId))
                .then(serializeToJson(PresenceEventFactory.leave(sessionId)))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .then();
    }

    public Mono<Void> notifyJoin(String projectId, String sessionId,
            String userId, String userName) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        return serializeToJson(
                PresenceEventFactory.join(sessionId, userId, userName))
                .flatMap(eventJson -> redisTemplate.convertAndSend(
                        channelName,
                        eventJson))
                .then();
    }

    /**
     * handle WebSocket messages (generic handler)
     */
    public Mono<Void> handleMessage(String projectId, String sessionId,
            String payload) {
        return deserializeFromJson(payload, ClientMessage.class)
                .flatMap(message -> {
                    PresenceEventType type = message.getType();
                    if (type == null) {
                        log.warn(
                                "[PresenceService] No message type: sessionId={}",
                                sessionId);
                        return Mono.empty();
                    }

                    return switch (type) {
                    case CURSOR -> {
                        if (!(message instanceof CursorClientMessage cursorMessage)) {
                            log.warn(
                                    "[PresenceService] Invalid message format: sessionId={}",
                                    sessionId);
                            yield Mono.empty();
                        }
                        CursorPosition cursor = cursorMessage.getCursor();
                        if (cursor == null) {
                            log.warn(
                                    "[PresenceService] No cursor data: sessionId={}",
                                    sessionId);
                            yield Mono.empty();
                        }
                        yield updateCursor(projectId, sessionId, cursor);
                    }
                    default -> {
                        log.warn(
                                "[PresenceService] Unhandled message type: type={}, sessionId={}",
                                type,
                                sessionId);
                        yield Mono.empty();
                    }
                    };
                })
                .onErrorResume(e -> {
                    log.warn("[PresenceService] Message handling failed: {}",
                            e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * handle Redis messages received from Redis and broadcast to local WebSocket clients
     */
    public Mono<Void> handleRedisMessage(String projectId, String message) {
        return deserializeFromJson(message, PresenceEvent.class)
                .flatMap(event -> serializeToJson(event.withoutSessionId())
                        .flatMap(json -> sessionService.broadcast(
                                projectId, event.getSessionId(), json)));
    }

    private <T> Mono<String> serializeToJson(T object) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(object))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[PresenceService] failed to serialize JSON",
                                e));
    }

    private <T> Mono<T> deserializeFromJson(String json, Class<T> clazz) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, clazz))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[PresenceService] failed to deserialize JSON",
                                e));
    }

}
