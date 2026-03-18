package com.schemafy.api.collaboration.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationPayloadSerializer {

  private final ObjectMapper objectMapper;

  public Mono<String> serialize(CollaborationOutbound event) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
        .onErrorMap(JsonProcessingException.class,
            e -> new RuntimeException(
                "[CollaborationPayloadSerializer] failed to serialize JSON",
                e));
  }

  public Mono<String> serializeForBroadcast(CollaborationOutbound event) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
        .onErrorMap(JsonProcessingException.class,
        e -> new RuntimeException(
            "[CollaborationPayloadSerializer] failed to serialize broadcast JSON",
            e));
  }

}
