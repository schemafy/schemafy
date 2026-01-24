package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface RemoveRelationshipColumnUseCase {

  Mono<Void> removeRelationshipColumn(RemoveRelationshipColumnCommand command);

}
