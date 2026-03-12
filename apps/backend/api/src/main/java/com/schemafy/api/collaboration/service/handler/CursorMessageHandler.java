package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.CursorPosition;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CursorEvent;
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
public class CursorMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.CURSOR;
  }

  @Override
  public Mono<Void> handle(MessageContext context,
      CollaborationInbound message) {
    if (!(message instanceof CursorEvent.Inbound cursorMessage)) {
      log.warn(
          "[CursorMessageHandler] Invalid message format: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    CursorPosition cursor = cursorMessage.cursor();
    if (cursor == null) {
      log.warn("[CursorMessageHandler] No cursor data: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    SessionEntry entry = sessionRegistry
        .getSessionEntry(context.projectId(), context.sessionId())
        .orElse(null);

    if (entry == null) {
      log.warn("[CursorMessageHandler] Session not found: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    entry.pushCursor(cursor);

    return Mono.empty();
  }

}
