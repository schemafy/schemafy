package com.schemafy.domain.erd.application.port.in;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipExtraUseCase {

  Mono<Void> changeRelationshipExtra(ChangeRelationshipExtraCommand command);

}
