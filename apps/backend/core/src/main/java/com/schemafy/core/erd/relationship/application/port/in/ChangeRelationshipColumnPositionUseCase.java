package com.schemafy.core.erd.relationship.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeRelationshipColumnPositionUseCase {

  Mono<MutationResult<Void>> changeRelationshipColumnPosition(
      ChangeRelationshipColumnPositionCommand command);

}
