package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeConstraintColumnPositionUseCase {

  Mono<MutationResult<Void>> changeConstraintColumnPosition(
      ChangeConstraintColumnPositionCommand command);

}
