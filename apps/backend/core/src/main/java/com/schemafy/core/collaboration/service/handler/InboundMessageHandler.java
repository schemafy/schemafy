package com.schemafy.core.collaboration.service.handler;

import com.schemafy.core.collaboration.dto.CollaborationEventType;
import com.schemafy.core.collaboration.dto.event.CollaborationInbound;

import reactor.core.publisher.Mono;

public interface InboundMessageHandler {

  CollaborationEventType supportedType();

  Mono<Void> handle(MessageContext context, CollaborationInbound message);

}
