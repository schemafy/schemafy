package com.schemafy.core.erd.operation.application.service;

import java.util.List;

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
import com.schemafy.core.erd.operation.application.inverse.ReorderPositions;
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
                        .changeIndexColumnPositions(
                            index.id(),
                            ReorderPositions.restore(
                                columns,
                                IndexColumn::id,
                                inversePayload.positions(),
                                ChangeIndexColumnPositionUndoRedoHandler::withSeqNo))
                        .thenReturn(MutationResult.<Void>of(null, index.tableId())
                            .withInverse(new ChangeIndexColumnPositionInverse(
                                indexColumn.id(),
                                ReorderPositions.capture(
                                    columns,
                                    IndexColumn::id,
                                    IndexColumn::seqNo))))))));
  }

  private static IndexColumn withSeqNo(IndexColumn column, int seqNo) {
    return new IndexColumn(
        column.id(),
        column.indexId(),
        column.columnId(),
        seqNo,
        column.sortDirection());
  }

}
