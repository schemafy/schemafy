package com.schemafy.api.collaboration.service.handler;

import com.schemafy.api.collaboration.dto.CollaborationEventType;
import com.schemafy.api.collaboration.dto.event.CollaborationInbound;

import reactor.core.publisher.Mono;

public interface InboundMessageHandler {

  CollaborationEventType supportedType();

  Mono<Void> handle(MessageContext context, CollaborationInbound message);

}
