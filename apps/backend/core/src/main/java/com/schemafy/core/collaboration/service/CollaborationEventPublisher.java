package com.schemafy.core.collaboration.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.schemafy.core.collaboration.CollaborationChannel;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
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
    return publishStrict(projectId, event)
        .doOnError(e -> log.warn(
            "[CollaborationEventPublisher] Failed to publish event: type={}, sessionId={}, error={}",
            event.type(), event.sessionId(), e.getMessage()))
        .onErrorResume(e -> Mono.empty());
  }

  public Mono<Void> publishStrict(String projectId,
      CollaborationOutbound event) {
    String channelName = CollaborationChannel.forProject(projectId);

    return serializeToJson(event)
        .flatMap(eventJson -> redisTemplate.convertAndSend(channelName,
            eventJson))
        .then();
  }

  private Mono<String> serializeToJson(Object object) {
    return Mono.fromCallable(() -> jsonCodec.toJson(object))
        .onErrorMap(IllegalArgumentException.class,
            e -> new RuntimeException("[CollaborationEventPublisher] Failed to serialize JSON",
                e));
  }

}
