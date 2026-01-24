package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteRelationshipUseCase {

  Mono<Void> deleteRelationship(DeleteRelationshipCommand command);

}
