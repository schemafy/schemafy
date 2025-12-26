package com.schemafy.core.collaboration.service.handler;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.dto.event.CollaborationInbound;
import com.schemafy.core.collaboration.dto.event.SchemaFocusEvent;
import com.schemafy.core.collaboration.service.SessionRegistry;
import com.schemafy.core.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class SchemaFocusMessageHandler implements InboundMessageHandler {

    private final SessionRegistry sessionRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public CollaborationEventType supportedType() {
        return CollaborationEventType.SCHEMA_FOCUS;
    }

    @Override
    public Mono<Void> handle(MessageContext context, CollaborationInbound message) {
        if (!(message instanceof SchemaFocusEvent.Inbound schemaFocusMessage)) {
            log.warn(
                    "[SchemaFocusMessageHandler] Invalid message format: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        String schemaId = schemaFocusMessage.schemaId();
        if (schemaId == null || schemaId.isBlank()) {
            log.warn(
                    "[SchemaFocusMessageHandler] No schemaId in message: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        SessionEntry entry = sessionRegistry
                .getSessionEntry(context.projectId(), context.sessionId())
                .orElse(null);

        if (entry == null) {
            log.warn("[SchemaFocusMessageHandler] Session not found: sessionId={}",
                    context.sessionId());
            return Mono.empty();
        }

        entry.setCurrentSchemaId(schemaId);

        String userId = entry.authInfo().getUserId();
        String userName = entry.authInfo().getUserName();
        String channelName = CollaborationConstants.CHANNEL_PREFIX
                + context.projectId();

        return serializeToJson(CollaborationOutboundFactory.schemaFocus(
                context.sessionId(), userId, userName, schemaId))
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .doOnError(e -> log.warn(
                        "[SchemaFocusMessageHandler] Failed to publish: sessionId={}, error={}",
                        context.sessionId(), e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<String> serializeToJson(Object object) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(object))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[SchemaFocusMessageHandler] Failed to serialize JSON",
                                e));
    }

}
