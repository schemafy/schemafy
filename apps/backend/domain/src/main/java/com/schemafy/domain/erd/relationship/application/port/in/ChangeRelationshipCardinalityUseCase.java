package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipCardinalityUseCase {

  Mono<Void> changeRelationshipCardinality(ChangeRelationshipCardinalityCommand command);

}
