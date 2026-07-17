package com.schemafy.core.erd.operation.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexColumnPositionInverse;
import com.schemafy.core.erd.operation.application.inverse.ReorderPosition;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeIndexColumnPositionUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeIndexColumnPositionInverse> {

  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  ChangeIndexColumnPositionUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeIndexColumnPositionPort changeIndexColumnPositionPort,
      GetIndexByIdPort getIndexByIdPort,
      GetIndexColumnByIdPort getIndexColumnByIdPort,
      GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort) {
    super(ErdOperationType.CHANGE_INDEX_COLUMN_POSITION,
        ChangeIndexColumnPositionInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeIndexColumnPositionPort = changeIndexColumnPositionPort;
    this.getIndexByIdPort = getIndexByIdPort;
    this.getIndexColumnByIdPort = getIndexColumnByIdPort;
    this.getIndexColumnsByIndexIdPort = getIndexColumnsByIndexIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeIndexColumnPositionInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return coordinate(resolved, inversePayload,
        () -> getIndexColumnByIdPort.findIndexColumnById(inversePayload.indexColumnId())
            .switchIfEmpty(Mono.error(new DomainException(
                IndexErrorCode.POSITION_INVALID,
                "Index column not found: " + inversePayload.indexColumnId())))
            .flatMap(indexColumn -> getIndexByIdPort.findIndexById(indexColumn.indexId())
                .switchIfEmpty(Mono.error(new DomainException(
                    IndexErrorCode.NOT_FOUND,
                    "Index not found: " + indexColumn.indexId())))
                .flatMap(index -> getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(index.id())
                    .defaultIfEmpty(List.of())
                    .flatMap(columns -> changeIndexColumnPositionPort
                        .changeIndexColumnPositions(index.id(), restorePositions(columns, inversePayload.positions()))
                        .thenReturn(MutationResult.<Void>of(null, index.tableId())
                            .withInverse(new ChangeIndexColumnPositionInverse(
                                indexColumn.id(),
                                toPositions(columns))))))));
  }

  private static List<IndexColumn> restorePositions(
      List<IndexColumn> columns,
      List<ReorderPosition> positions) {
    Map<String, Integer> positionsById = positionsById(columns.size(), positions);
    return columns.stream()
        .map(column -> new IndexColumn(
            column.id(),
            column.indexId(),
            column.columnId(),
            requirePosition(positionsById, column.id()),
            column.sortDirection()))
        .toList();
  }

  private static List<ReorderPosition> toPositions(List<IndexColumn> columns) {
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
    return new IllegalStateException("Index column reorder snapshot does not match current columns");
  }

}
