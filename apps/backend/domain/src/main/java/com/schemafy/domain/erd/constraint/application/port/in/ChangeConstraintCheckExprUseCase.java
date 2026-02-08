package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeConstraintCheckExprUseCase {

  Mono<MutationResult<Void>> changeConstraintCheckExpr(ChangeConstraintCheckExprCommand command);

}
