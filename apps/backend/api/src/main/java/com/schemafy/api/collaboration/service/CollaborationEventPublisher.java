package com.schemafy.api.collaboration.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.api.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationEventPublisher {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final JsonCodec jsonCodec;

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
    return Mono.fromCallable(() -> jsonCodec.serialize(object))
        .onErrorMap(IllegalArgumentException.class,
            e -> new RuntimeException("[CollaborationEventPublisher] Failed to serialize JSON",
                e));
  }

}
