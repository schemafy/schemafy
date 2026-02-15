package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.MutationResult;

import reactor.core.publisher.Mono;

public interface ChangeConstraintDefaultExprUseCase {

  Mono<MutationResult<Void>> changeConstraintDefaultExpr(ChangeConstraintDefaultExprCommand command);

}
