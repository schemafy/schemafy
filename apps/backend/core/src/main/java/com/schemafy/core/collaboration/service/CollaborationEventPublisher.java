package com.schemafy.core.collaboration.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.constant.CollaborationConstants;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationEventPublisher {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Mono<Void> publish(String projectId, CollaborationOutbound event) {
        String channelName = CollaborationConstants.CHANNEL_PREFIX + projectId;

        return serializeToJson(event)
                .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
                        eventJson))
                .doOnError(e -> log.warn(
                        "[CollaborationEventPublisher] Failed to publish event: type={}, sessionId={}, error={}",
                        event.type(), event.sessionId(), e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<String> serializeToJson(Object object) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(object))
                .onErrorMap(JsonProcessingException.class,
                        e -> new RuntimeException(
                                "[CollaborationEventPublisher] Failed to serialize JSON",
                                e));
    }

}
