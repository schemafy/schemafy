package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.RelationshipExtraPreviewEvent;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class RelationshipExtraPreviewMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;
  private final CollaborationEventPublisher eventPublisher;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.RELATIONSHIP_EXTRA_PREVIEW;
  }

  @Override
  public Mono<Void> handle(MessageContext context,
      CollaborationInbound message) {
    if (!(message instanceof RelationshipExtraPreviewEvent.Inbound previewMessage)) {
      log.warn(
          "[RelationshipExtraPreviewMessageHandler] Invalid message format: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    PreviewAction action = previewMessage.action();
    if (action == null) {
      log.warn(
          "[RelationshipExtraPreviewMessageHandler] No action in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String schemaId = previewMessage.schemaId();
    if (schemaId == null || schemaId.isBlank()) {
      log.warn(
          "[RelationshipExtraPreviewMessageHandler] No schemaId in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String relationshipId = previewMessage.relationshipId();
    if (relationshipId == null || relationshipId.isBlank()) {
      log.warn(
          "[RelationshipExtraPreviewMessageHandler] No relationshipId in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    if (sessionRegistry.getSessionEntry(context.projectId(), context.sessionId())
        .isEmpty()) {
      log.warn(
          "[RelationshipExtraPreviewMessageHandler] Session not found: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    JsonNode extra = null;
    if (action == PreviewAction.UPDATE) {
      extra = previewMessage.extra();
      if (extra == null || !extra.isObject()) {
        log.warn(
            "[RelationshipExtraPreviewMessageHandler] Invalid extra payload: sessionId={}",
            context.sessionId());
        return Mono.empty();
      }
    }

    return eventPublisher.publish(context.projectId(),
        CollaborationOutboundFactory.relationshipExtraPreview(
            context.sessionId(),
            action,
            schemaId,
            relationshipId,
            extra));
  }

}
