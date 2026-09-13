package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexColumnSortDirectionInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeIndexColumnSortDirectionUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeIndexColumnSortDirectionInverse> {

  private final ChangeIndexColumnSortDirectionPort changeIndexColumnSortDirectionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;

  ChangeIndexColumnSortDirectionUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeIndexColumnSortDirectionPort changeIndexColumnSortDirectionPort,
      GetIndexColumnByIdPort getIndexColumnByIdPort,
      GetIndexByIdPort getIndexByIdPort) {
    super(ErdOperationType.CHANGE_INDEX_COLUMN_SORT_DIRECTION, ChangeIndexColumnSortDirectionInverse.class,
        jsonCodec, erdMutationCoordinator);
    this.changeIndexColumnSortDirectionPort = changeIndexColumnSortDirectionPort;
    this.getIndexColumnByIdPort = getIndexColumnByIdPort;
    this.getIndexByIdPort = getIndexByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeIndexColumnSortDirectionInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getIndexColumnByIdPort.findIndexColumnById(inversePayload.indexColumnId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.COLUMN_NOT_FOUND,
            "Index column not found: " + inversePayload.indexColumnId())))
        .flatMap(indexColumn -> getIndexByIdPort.findIndexById(indexColumn.indexId())
            .switchIfEmpty(Mono.error(new DomainException(
                IndexErrorCode.NOT_FOUND,
                "Index not found: " + indexColumn.indexId())))
            .flatMap(index -> coordinate(resolved, inversePayload,
                () -> changeIndexColumnSortDirectionPort.changeIndexColumnSortDirection(
                    inversePayload.indexColumnId(),
                    inversePayload.oldSortDirection())
                    .thenReturn(MutationResult.<Void>of(null, index.tableId())
                        .withInverse(new ChangeIndexColumnSortDirectionInverse(
                            indexColumn.id(),
                            indexColumn.sortDirection()))))));
  }

}
