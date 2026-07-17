package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableMetaInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.table.application.port.out.ChangeTableMetaPort;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import reactor.core.publisher.Mono;

@Component
class ChangeTableMetaUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeTableMetaInverse> {

  private final ChangeTableMetaPort changeTableMetaPort;
  private final GetTableByIdPort getTableByIdPort;

  ChangeTableMetaUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeTableMetaPort changeTableMetaPort,
      GetTableByIdPort getTableByIdPort) {
    super(ErdOperationType.CHANGE_TABLE_META, ChangeTableMetaInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeTableMetaPort = changeTableMetaPort;
    this.getTableByIdPort = getTableByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeTableMetaInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getTableByIdPort.findTableById(inversePayload.tableId())
        .switchIfEmpty(Mono.error(new DomainException(
            TableErrorCode.NOT_FOUND,
            "Table not found: " + inversePayload.tableId())))
        .flatMap(table -> coordinate(resolved, inversePayload,
            () -> changeTableMetaPort.changeTableMeta(
                inversePayload.tableId(),
                inversePayload.oldCharset(),
                inversePayload.oldCollation())
                .thenReturn(MutationResult.<Void>of(null, table.id())
                    .withInverse(new ChangeTableMetaInverse(
                        table.id(),
                        inversePayload.oldCharset() == null
                            ? null
                            : Objects.toString(table.charset(), ""),
                        inversePayload.oldCollation() == null
                            ? null
                            : Objects.toString(table.collation(), ""))))));
  }

}
