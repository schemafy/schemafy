package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipExtraUseCase {

  Mono<Void> changeRelationshipExtra(ChangeRelationshipExtraCommand command);

}
