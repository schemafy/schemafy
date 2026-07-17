package com.schemafy.core.erd.operation.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPosition;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeColumnPositionUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnPositionInverse> {

  private final ChangeColumnPositionPort changeColumnPositionPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  ChangeColumnPositionUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnPositionPort changeColumnPositionPort,
      GetColumnByIdPort getColumnByIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    super(ErdOperationType.CHANGE_COLUMN_POSITION, ChangeColumnPositionInverse.class, jsonCodec,
        erdMutationCoordinator);
    this.changeColumnPositionPort = changeColumnPositionPort;
    this.getColumnByIdPort = getColumnByIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnPositionInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return coordinate(resolved, inversePayload,
        () -> getColumnByIdPort.findColumnById(inversePayload.columnId())
            .switchIfEmpty(Mono.error(new DomainException(
                ColumnErrorCode.NOT_FOUND,
                "Column not found: " + inversePayload.columnId())))
            .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
                .defaultIfEmpty(List.of())
                .flatMap(columns -> changeColumnPositionPort
                    .changeColumnPositions(column.tableId(), restorePositions(columns, inversePayload.positions()))
                    .thenReturn(MutationResult.<Void>of(null, column.tableId())
                        .withInverse(new ChangeColumnPositionInverse(
                            column.id(),
                            toPositions(columns)))))));
  }

  private static List<Column> restorePositions(
      List<Column> columns,
      List<ReorderPosition> positions) {
    Map<String, Integer> positionsById = positionsById(columns.size(), positions);
    return columns.stream()
        .map(column -> new Column(
            column.id(),
            column.tableId(),
            column.name(),
            column.dataType(),
            column.typeArguments(),
            requirePosition(positionsById, column.id()),
            column.autoIncrement(),
            column.charset(),
            column.collation(),
            column.comment()))
        .toList();
  }

  private static List<ReorderPosition> toPositions(List<Column> columns) {
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
    return new IllegalStateException("Column reorder snapshot does not match current columns");
  }

}
