package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.event.ChatEvent;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;
import com.schemafy.api.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.ulid.application.port.in.GenerateUlidUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ChatMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;
  private final CollaborationEventPublisher eventPublisher;
  private final GenerateUlidUseCase generateUlidUseCase;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.CHAT;
  }

  @Override
  public Mono<Void> handle(MessageContext context,
      CollaborationInbound message) {
    if (!(message instanceof ChatEvent.Inbound chatMessage)) {
      log.warn(
          "[ChatMessageHandler] Invalid message format: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String content = chatMessage.content();
    if (content == null || content.isBlank()) {
      log.warn("[ChatMessageHandler] Empty chat content: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    SessionEntry entry = sessionRegistry
        .getSessionEntry(context.projectId(), context.sessionId())
        .orElse(null);

    if (entry == null) {
      log.warn("[ChatMessageHandler] Session not found: sessionId={}",
          context.sessionId());
      return Mono.empty();
    }

    String authorId = entry.authInfo().getUserId();
    String authorName = entry.authInfo().getUserName();
    return generateUlidUseCase.generateUlid()
        .flatMap(messageId -> eventPublisher.publish(context.projectId(),
            CollaborationOutboundFactory.chat(context.sessionId(),
                messageId, authorId, authorName, content))
            .doOnSuccess(v -> log.debug(
                "[ChatMessageHandler] Message published: messageId={}, projectId={}",
                messageId, context.projectId())));
  }

}
