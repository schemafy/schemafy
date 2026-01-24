package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionUseCase {

  Mono<Void> changeRelationshipColumnPosition(ChangeRelationshipColumnPositionCommand command);

}
