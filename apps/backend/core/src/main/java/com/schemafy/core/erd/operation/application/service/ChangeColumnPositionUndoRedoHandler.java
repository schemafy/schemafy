package com.schemafy.core.erd.operation.application.service;

import java.util.List;

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
import com.schemafy.core.erd.operation.application.inverse.ReorderPositions;
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
                    .changeColumnPositions(
                        column.tableId(),
                        ReorderPositions.restore(
                            columns,
                            Column::id,
                            inversePayload.positions(),
                            Column::withSeqNo))
                    .thenReturn(MutationResult.<Void>of(null, column.tableId())
                        .withInverse(new ChangeColumnPositionInverse(
                            column.id(),
                            ReorderPositions.capture(
                                columns,
                                Column::id,
                                Column::seqNo)))))));
  }

}
