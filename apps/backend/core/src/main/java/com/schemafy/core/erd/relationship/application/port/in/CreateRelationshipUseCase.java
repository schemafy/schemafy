package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface CreateRelationshipUseCase {

  Mono<MutationResult<CreateRelationshipResult>> createRelationship(
      CreateRelationshipCommand command);

}
