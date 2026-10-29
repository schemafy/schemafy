package com.schemafy.api.collaboration.service.handler;

import org.springframework.stereotype.Component;

import com.schemafy.api.collaboration.security.ProjectAccessValidator;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.event.CanvasEditLockEvent;
import com.schemafy.core.collaboration.dto.event.CollaborationInbound;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.lock.CanvasEditLockService;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class CanvasEditLockMessageHandler implements InboundMessageHandler {

  private final SessionRegistry sessionRegistry;
  private final ProjectAccessValidator projectAccessValidator;
  private final CanvasEditLockService lockService;
  private final CollaborationEventPublisher eventPublisher;

  @Override
  public CollaborationEventType supportedType() {
    return CollaborationEventType.CANVAS_EDIT_LOCK;
  }

  @Override
  public Mono<Void> handle(MessageContext context, CollaborationInbound message) {
    if (!(message instanceof CanvasEditLockEvent.Inbound lockMessage)
        || lockMessage.action() == null || lockMessage.target() == null
        || blank(lockMessage.schemaId()) || blank(lockMessage.resourceId())) {
      return Mono.empty();
    }
    return Mono.justOrEmpty(sessionRegistry.getSessionEntry(context.projectId(), context.sessionId()))
        .flatMap(entry -> projectAccessValidator.canEdit(context.projectId(),
            entry.authInfo().getUserId())
            .filter(Boolean::booleanValue)
            .flatMap(ignored -> apply(context, lockMessage)))
        .then();
  }

  private Mono<Void> apply(MessageContext context, CanvasEditLockEvent.Inbound message) {
    Mono<com.schemafy.core.collaboration.lock.CanvasEditLockResult> result = switch (message.action()) {
    case ACQUIRE -> lockService.acquire(context.projectId(), message.target(),
        message.resourceId(), context.sessionId());
    case RENEW -> lockService.renew(context.projectId(), message.target(),
        message.resourceId(), context.sessionId());
    case RELEASE -> lockService.release(context.projectId(), message.target(),
        message.resourceId(), context.sessionId());
    };
    return result.flatMap(lock -> eventPublisher.publish(context.projectId(),
        CollaborationOutboundFactory.canvasEditLock(context.sessionId(), lock.state(),
            message.target(), message.schemaId(), message.resourceId(), lock.ownerSessionId())));
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

}
