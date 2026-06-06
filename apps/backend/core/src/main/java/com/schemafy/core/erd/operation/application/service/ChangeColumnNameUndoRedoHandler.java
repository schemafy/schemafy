package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeColumnNameUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeColumnNameInverse> {

  private final ChangeColumnNamePort changeColumnNamePort;
  private final GetColumnByIdPort getColumnByIdPort;

  ChangeColumnNameUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeColumnNamePort changeColumnNamePort,
      GetColumnByIdPort getColumnByIdPort) {
    super(ErdOperationType.CHANGE_COLUMN_NAME, ChangeColumnNameInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeColumnNamePort = changeColumnNamePort;
    this.getColumnByIdPort = getColumnByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeColumnNameInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getColumnByIdPort.findColumnById(inversePayload.columnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ColumnErrorCode.NOT_FOUND,
            "Column not found: " + inversePayload.columnId())))
        .flatMap(column -> coordinate(resolved, inversePayload,
            () -> changeColumnNamePort.changeColumnName(inversePayload.columnId(), inversePayload.oldName())
                .thenReturn(MutationResult.<Void>of(null, column.tableId())
                    .withInverse(new ChangeColumnNameInverse(column.id(), column.name())))));
  }

}
