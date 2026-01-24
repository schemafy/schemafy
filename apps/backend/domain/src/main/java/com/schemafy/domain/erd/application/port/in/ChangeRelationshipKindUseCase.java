package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipKindUseCase {

  Mono<Void> changeRelationshipKind(ChangeRelationshipKindCommand command);

}
