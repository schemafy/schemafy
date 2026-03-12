package com.schemafy.core.erd.constraint.application.port.in;

import com.schemafy.core.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeConstraintDefaultExprUseCase {

  Mono<MutationResult<Void>> changeConstraintDefaultExpr(ChangeConstraintDefaultExprCommand command);

}
