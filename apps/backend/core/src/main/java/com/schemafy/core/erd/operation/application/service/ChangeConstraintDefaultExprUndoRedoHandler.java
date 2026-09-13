package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintExpressionPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintDefaultExprInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeConstraintDefaultExprUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeConstraintDefaultExprInverse> {

  private final ChangeConstraintExpressionPort changeConstraintExpressionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;

  ChangeConstraintDefaultExprUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeConstraintExpressionPort changeConstraintExpressionPort,
      GetConstraintByIdPort getConstraintByIdPort) {
    super(ErdOperationType.CHANGE_CONSTRAINT_DEFAULT_EXPR, ChangeConstraintDefaultExprInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeConstraintExpressionPort = changeConstraintExpressionPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeConstraintDefaultExprInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getConstraintByIdPort.findConstraintById(inversePayload.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            "Constraint not found: " + inversePayload.constraintId())))
        .flatMap(constraint -> coordinate(resolved, inversePayload,
            () -> changeConstraintExpressionPort.changeConstraintExpressions(
                inversePayload.constraintId(),
                constraint.checkExpr(),
                inversePayload.oldDefaultExpr())
                .thenReturn(MutationResult.<Void>of(null, constraint.tableId())
                    .withInverse(new ChangeConstraintDefaultExprInverse(
                        constraint.id(),
                        constraint.defaultExpr())))));
  }

}
