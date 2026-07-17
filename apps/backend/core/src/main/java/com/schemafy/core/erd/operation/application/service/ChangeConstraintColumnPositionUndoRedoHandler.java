package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPositions;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeConstraintColumnPositionUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeConstraintColumnPositionInverse> {

  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  ChangeConstraintColumnPositionUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetConstraintColumnByIdPort getConstraintColumnByIdPort,
      GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort) {
    super(ErdOperationType.CHANGE_CONSTRAINT_COLUMN_POSITION,
        ChangeConstraintColumnPositionInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeConstraintColumnPositionPort = changeConstraintColumnPositionPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getConstraintColumnByIdPort = getConstraintColumnByIdPort;
    this.getConstraintColumnsByConstraintIdPort = getConstraintColumnsByConstraintIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeConstraintColumnPositionInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return coordinate(resolved, inversePayload,
        () -> getConstraintColumnByIdPort
            .findConstraintColumnById(inversePayload.constraintColumnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ConstraintErrorCode.COLUMN_NOT_FOUND,
                "Constraint column not found: " + inversePayload.constraintColumnId())))
            .flatMap(constraintColumn -> getConstraintByIdPort
                .findConstraintById(constraintColumn.constraintId())
                .switchIfEmpty(Mono.error(new DomainException(
                    ConstraintErrorCode.NOT_FOUND,
                    "Constraint not found: " + constraintColumn.constraintId())))
                .flatMap(constraint -> getConstraintColumnsByConstraintIdPort
                    .findConstraintColumnsByConstraintId(constraint.id())
                    .defaultIfEmpty(List.of())
                    .flatMap(columns -> changeConstraintColumnPositionPort
                        .changeConstraintColumnPositions(
                            constraint.id(),
                            ReorderPositions.restore(
                                columns,
                                ConstraintColumn::id,
                                inversePayload.positions(),
                                ChangeConstraintColumnPositionUndoRedoHandler::withSeqNo))
                        .thenReturn(MutationResult.<Void>of(null, constraint.tableId())
                            .withInverse(new ChangeConstraintColumnPositionInverse(
                                constraintColumn.id(),
                                ReorderPositions.capture(
                                    columns,
                                    ConstraintColumn::id,
                                    ConstraintColumn::seqNo))))))));
  }

  private static ConstraintColumn withSeqNo(
      ConstraintColumn column,
      int seqNo) {
    return new ConstraintColumn(
        column.id(),
        column.constraintId(),
        column.columnId(),
        seqNo);
  }

}
