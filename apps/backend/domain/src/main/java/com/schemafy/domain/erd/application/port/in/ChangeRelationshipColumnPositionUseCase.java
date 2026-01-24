package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionUseCase {

  Mono<Void> changeRelationshipColumnPosition(ChangeRelationshipColumnPositionCommand command);

}
