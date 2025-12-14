package com.schemafy.core.collaboration.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.BroadcastMessage;
import com.schemafy.core.collaboration.dto.ClientMessage;
import com.schemafy.core.collaboration.dto.CursorClientMessage;
import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.dto.PresenceEvent;
import com.schemafy.core.collaboration.dto.PresenceEventFactory;
import com.schemafy.core.collaboration.dto.PresenceEventType;
import com.schemafy.core.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class PresenceService {

    private static final double CURSOR_POSITION_EPS = 0.5;

    private final SessionService sessionService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, CursorPosition> cursorDedupeCache = new ConcurrentHashMap<>();

    public void pushCursorToSink(String projectId, String sessionId,
            CursorPosition cursor) {
        SessionEntry entry = sessionService
                .getSessionEntry(projectId, sessionId)
                .orElse(null);

        if (entry == null) {
            log.warn(
                    "[PresenceService] Session not found for cursor push: sessionId={}",
                    sessionId);
            return;
        }

        String userName = entry.authInfo().getUserName();
        CursorPosition cursorWithUserName = cursor.withUserName(userName);

        entry.pushCursor(cursorWithUserName);
    }

    public Mono<Void> publishCursorToRedis(String projectId, String sessionId,
            CursorPosition cursor) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        CursorPosition previousCursor = cursorDedupeCache.get(sessionId);
        if (isDuplicateCursor(previousCursor, cursor)) {
            return Mono.empty();
        }
        cursorDedupeCache.put(sessionId, cursor);

        return serializeToJson(PresenceEventFactory.cursor(sessionId, cursor))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .doOnError(e -> log.warn(
                        "[PresenceService] Failed to publish cursor: sessionId={}, error={}",
                        sessionId, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    public Mono<Void> removeSession(String projectId, String sessionId) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;
        cursorDedupeCache.remove(sessionId);

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
                            yield Mono.<Void>empty();
                        }
                        CursorPosition cursor = cursorMessage.getCursor();
                        if (cursor == null) {
                            log.warn(
                                    "[PresenceService] No cursor data: sessionId={}",
                                    sessionId);
                            yield Mono.<Void>empty();
                        }
                        pushCursorToSink(projectId, sessionId, cursor);
                        yield Mono.<Void>empty();
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
     * handle Redis messages received from Redis and broadcast to local WebSocket clients.
     */
    public Mono<Void> handleRedisMessage(String projectId, String message) {
        return deserializeFromJson(message, PresenceEvent.class)
                .flatMap(event -> serializeToJson(event.withoutSessionId())
                        .doOnNext(json -> broadcastByEventType(
                                projectId,
                                event.getSessionId(),
                                event.getType(),
                                json))
                        .then());
    }

    private void broadcastByEventType(String projectId, String excludeSessionId,
            PresenceEventType eventType, String json) {
        if (eventType == null) {
            log.warn("[PresenceService] Event type is null, using best effort");
            sessionService.broadcast(
                    BroadcastMessage.of(projectId, excludeSessionId, json));
            return;
        }

        switch (eventType) {
        case JOIN, LEAVE -> {
            sessionService.broadcast(
                    BroadcastMessage.of(projectId, excludeSessionId, json));
        }
        case CURSOR -> {
            sessionService.broadcast(
                    BroadcastMessage.of(projectId, excludeSessionId, json));
        }
        default -> {
            log.warn(
                    "[PresenceService] Unknown event type: {}, using best effort",
                    eventType);
            sessionService.broadcast(
                    BroadcastMessage.of(projectId, excludeSessionId, json));
        }
        }
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

    private boolean isDuplicateCursor(CursorPosition first,
            CursorPosition second) {
        if (first == null || second == null) {
            return false;
        }

        return Math.abs(first.x() - second.x()) < CURSOR_POSITION_EPS
                && Math.abs(first.y() - second.y()) < CURSOR_POSITION_EPS;
    }

}
