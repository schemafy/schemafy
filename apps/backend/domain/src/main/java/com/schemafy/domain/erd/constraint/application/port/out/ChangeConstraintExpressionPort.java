package com.schemafy.domain.erd.constraint.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeConstraintExpressionPort {

  Mono<Void> changeConstraintExpressions(String constraintId, String checkExpr, String defaultExpr);

}
