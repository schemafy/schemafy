package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeConstraintNameUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeConstraintNameInverse> {

  private final ChangeConstraintNamePort changeConstraintNamePort;
  private final GetConstraintByIdPort getConstraintByIdPort;

  ChangeConstraintNameUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeConstraintNamePort changeConstraintNamePort,
      GetConstraintByIdPort getConstraintByIdPort) {
    super(ErdOperationType.CHANGE_CONSTRAINT_NAME, ChangeConstraintNameInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeConstraintNamePort = changeConstraintNamePort;
    this.getConstraintByIdPort = getConstraintByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeConstraintNameInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getConstraintByIdPort.findConstraintById(inversePayload.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            "Constraint not found: " + inversePayload.constraintId())))
        .flatMap(constraint -> coordinate(resolved, inversePayload,
            () -> changeConstraintNamePort.changeConstraintName(
                    inversePayload.constraintId(),
                    inversePayload.oldName())
                .thenReturn(MutationResult.<Void>of(null, constraint.tableId())
                    .withInverse(new ChangeConstraintNameInverse(
                        constraint.id(),
                        constraint.name())))));
  }

}
