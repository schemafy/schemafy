package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableExtraInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.out.ChangeTableExtraPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Mono;

@Component
class ChangeTableExtraUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeTableExtraInverse> {

  private final ChangeTableExtraPort changeTableExtraPort;
  private final GetTableByIdPort getTableByIdPort;

  ChangeTableExtraUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeTableExtraPort changeTableExtraPort,
      GetTableByIdPort getTableByIdPort) {
    super(ErdOperationType.CHANGE_TABLE_EXTRA, ChangeTableExtraInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeTableExtraPort = changeTableExtraPort;
    this.getTableByIdPort = getTableByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeTableExtraInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getTableByIdPort.findTableById(inversePayload.tableId())
        .switchIfEmpty(Mono.error(new DomainException(
            TableErrorCode.NOT_FOUND,
            "Table not found: " + inversePayload.tableId())))
        .flatMap(table -> coordinate(resolved, inversePayload,
            () -> changeTableExtraPort.changeTableExtra(inversePayload.tableId(), inversePayload.oldExtra())
                .thenReturn(MutationResult.<Void>of(null, table.id())
                    .withInverse(new ChangeTableExtraInverse(table.id(), table.extra())))));
  }

}
