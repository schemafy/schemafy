package com.schemafy.domain.erd.relationship.application.port.in;

import reactor.core.publisher.Mono;

public interface CreateRelationshipUseCase {

  Mono<CreateRelationshipResult> createRelationship(CreateRelationshipCommand command);

}
