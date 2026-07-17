package com.schemafy.core.erd.operation.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.schemafy.core.erd.operation.application.inverse.ReorderPosition;
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
                            constraint.id(), restorePositions(columns, inversePayload.positions()))
                        .thenReturn(MutationResult.<Void>of(null, constraint.tableId())
                            .withInverse(new ChangeConstraintColumnPositionInverse(
                                constraintColumn.id(),
                                toPositions(columns))))))));
  }

  private static List<ConstraintColumn> restorePositions(
      List<ConstraintColumn> columns,
      List<ReorderPosition> positions) {
    Map<String, Integer> positionsById = positionsById(columns.size(), positions);
    return columns.stream()
        .map(column -> new ConstraintColumn(
            column.id(),
            column.constraintId(),
            column.columnId(),
            requirePosition(positionsById, column.id())))
        .toList();
  }

  private static List<ReorderPosition> toPositions(List<ConstraintColumn> columns) {
    return columns.stream()
        .map(column -> new ReorderPosition(column.id(), column.seqNo()))
        .toList();
  }

  private static Map<String, Integer> positionsById(
      int currentSize,
      List<ReorderPosition> positions) {
    if (positions.size() != currentSize) {
      throw snapshotMismatch();
    }
    Map<String, Integer> positionsById = new HashMap<>(positions.size());
    for (ReorderPosition position : positions) {
      if (positionsById.put(position.entityId(), position.seqNo()) != null) {
        throw snapshotMismatch();
      }
    }
    return positionsById;
  }

  private static int requirePosition(Map<String, Integer> positionsById, String entityId) {
    Integer position = positionsById.get(entityId);
    if (position == null) {
      throw snapshotMismatch();
    }
    return position;
  }

  private static IllegalStateException snapshotMismatch() {
    return new IllegalStateException(
        "Constraint column reorder snapshot does not match current columns");
  }

}
