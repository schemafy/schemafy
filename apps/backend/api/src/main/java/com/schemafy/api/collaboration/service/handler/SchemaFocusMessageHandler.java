package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.SchemaFocusEvent;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class SchemaFocusMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;
  private final CollaborationEventPublisher eventPublisher;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.SCHEMA_FOCUS;
  }

  @Override
  public Mono<Void> handle(MessageContext context,
      CollaborationInbound message) {
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
      log.warn(
          "[SchemaFocusMessageHandler] Session not found: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    entry.setCurrentSchemaId(schemaId);

    String userId = entry.authInfo().getUserId();
    String userName = entry.authInfo().getUserName();

    return eventPublisher.publish(context.projectId(),
        CollaborationOutboundFactory.schemaFocus(context.sessionId(),
            userId, userName, schemaId));
  }

}
