package com.schemafy.core.collaboration.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnRedisEnabled
public class CollaborationPayloadSerializer {

  private static final String SESSION_ID_FIELD = "sessionId";

  private final ObjectMapper objectMapper;

  public CollaborationPayloadSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<String> serialize(CollaborationOutbound event) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
        .onErrorMap(JsonProcessingException.class,
            e -> new RuntimeException(
                "[CollaborationPayloadSerializer] failed to serialize JSON",
                e));
  }

  public Mono<String> serializeForBroadcast(CollaborationOutbound event) {
    return Mono.fromCallable(() -> {
      ObjectNode payload = objectMapper.valueToTree(event);
      payload.remove(SESSION_ID_FIELD);
      return objectMapper.writeValueAsString(payload);
    }).onErrorMap(JsonProcessingException.class,
        e -> new RuntimeException(
            "[CollaborationPayloadSerializer] failed to serialize broadcast JSON",
            e));
  }

}
