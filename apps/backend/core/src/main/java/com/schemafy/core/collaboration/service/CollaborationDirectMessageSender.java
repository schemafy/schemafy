package com.schemafy.core.collaboration.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.service.model.SessionEntry;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CollaborationDirectMessageSender {

  private final CollaborationPayloadSerializer payloadSerializer;

  public Mono<Void> sendSessionReady(SessionEntry entry, String sessionId) {
    return send(entry, CollaborationOutboundFactory.sessionReady(sessionId));
  }

  public Mono<Void> send(SessionEntry entry, CollaborationOutbound event) {
    return payloadSerializer.serialize(event)
        .flatMap(payload -> emit(entry, payload));
  }

  private Mono<Void> emit(SessionEntry entry, String payload) {
    return Mono.fromRunnable(() -> {
      Sinks.EmitResult result = entry.send(payload);
      if (!result.isSuccess()) {
        throw new IllegalStateException(
            "[CollaborationDirectMessageSender] failed to emit direct message: "
                + result);
      }
    });
  }

}
