package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintExpressionPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintCheckExprInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeConstraintCheckExprUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeConstraintCheckExprInverse> {

  private final ChangeConstraintExpressionPort changeConstraintExpressionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;

  ChangeConstraintCheckExprUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeConstraintExpressionPort changeConstraintExpressionPort,
      GetConstraintByIdPort getConstraintByIdPort) {
    super(ErdOperationType.CHANGE_CONSTRAINT_CHECK_EXPR, ChangeConstraintCheckExprInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeConstraintExpressionPort = changeConstraintExpressionPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeConstraintCheckExprInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getConstraintByIdPort.findConstraintById(inversePayload.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            "Constraint not found: " + inversePayload.constraintId())))
        .flatMap(constraint -> coordinate(resolved, inversePayload,
            () -> changeConstraintExpressionPort.changeConstraintExpressions(
                inversePayload.constraintId(),
                inversePayload.oldCheckExpr(),
                constraint.defaultExpr())
                .thenReturn(MutationResult.<Void>of(null, constraint.tableId())
                    .withInverse(new ChangeConstraintCheckExprInverse(
                        constraint.id(),
                        constraint.checkExpr())))));
  }

}
