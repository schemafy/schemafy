package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.PreviewAction;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.dto.event.TablePositionPreviewEvent;
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
public class TablePositionPreviewMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;
  private final CollaborationEventPublisher eventPublisher;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.TABLE_POSITION_PREVIEW;
  }

  @Override
  public Mono<Void> handle(MessageContext context,
      CollaborationInbound message) {
    if (!(message instanceof TablePositionPreviewEvent.Inbound previewMessage)) {
      log.warn(
          "[TablePositionPreviewMessageHandler] Invalid message format: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    PreviewAction action = previewMessage.action();
    if (action == null) {
      log.warn(
          "[TablePositionPreviewMessageHandler] No action in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String schemaId = previewMessage.schemaId();
    if (schemaId == null || schemaId.isBlank()) {
      log.warn(
          "[TablePositionPreviewMessageHandler] No schemaId in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String tableId = previewMessage.tableId();
    if (tableId == null || tableId.isBlank()) {
      log.warn(
          "[TablePositionPreviewMessageHandler] No tableId in message: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    if (sessionRegistry.getSessionEntry(context.projectId(), context.sessionId())
        .isEmpty()) {
      log.warn(
          "[TablePositionPreviewMessageHandler] Session not found: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    JsonNode position = null;
    if (action == PreviewAction.UPDATE) {
      position = previewMessage.position();
      if (position == null || !position.isObject()) {
        log.warn(
            "[TablePositionPreviewMessageHandler] Invalid position payload: sessionId={}",
            context.sessionId());
        return Mono.empty();
      }
    }

    return eventPublisher.publish(context.projectId(),
        CollaborationOutboundFactory.tablePositionPreview(
            context.sessionId(),
            action,
            schemaId,
            tableId,
            position));
  }

}
