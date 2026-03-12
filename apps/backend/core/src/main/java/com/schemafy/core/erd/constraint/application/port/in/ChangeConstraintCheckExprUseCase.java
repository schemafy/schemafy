package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeConstraintCheckExprUseCase {

  Mono<MutationResult<Void>> changeConstraintCheckExpr(ChangeConstraintCheckExprCommand command);

}
