package com.schemafy.api.collaboration.service;

import org.springframework.stereotype.Component;

import com.schemafy.api.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationPayloadSerializer {

  private final JsonCodec jsonCodec;

  public Mono<String> serialize(CollaborationOutbound event) {
    return Mono.fromCallable(() -> jsonCodec.serialize(event))
        .onErrorMap(IllegalArgumentException.class,
            e -> new RuntimeException("[CollaborationPayloadSerializer] failed to serialize JSON",
                e));
  }

}
