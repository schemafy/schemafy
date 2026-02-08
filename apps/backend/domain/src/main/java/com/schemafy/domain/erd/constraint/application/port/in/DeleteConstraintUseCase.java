package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface DeleteConstraintUseCase {

  Mono<MutationResult<Void>> deleteConstraint(DeleteConstraintCommand command);

}
