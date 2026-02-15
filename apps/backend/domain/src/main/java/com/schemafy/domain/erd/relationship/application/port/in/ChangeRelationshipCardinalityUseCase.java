package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipCardinalityUseCase {

  Mono<MutationResult<Void>> changeRelationshipCardinality(
      ChangeRelationshipCardinalityCommand command);

}
