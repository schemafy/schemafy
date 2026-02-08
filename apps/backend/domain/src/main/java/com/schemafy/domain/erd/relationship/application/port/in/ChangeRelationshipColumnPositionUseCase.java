package com.schemafy.domain.erd.relationship.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionUseCase {

  Mono<MutationResult<Void>> changeRelationshipColumnPosition(
      ChangeRelationshipColumnPositionCommand command);

}
