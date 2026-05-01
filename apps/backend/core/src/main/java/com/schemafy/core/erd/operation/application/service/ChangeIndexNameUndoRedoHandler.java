package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexNamePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexNameInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeIndexNameUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeIndexNameInverse> {

  private final ChangeIndexNamePort changeIndexNamePort;
  private final GetIndexByIdPort getIndexByIdPort;

  ChangeIndexNameUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeIndexNamePort changeIndexNamePort,
      GetIndexByIdPort getIndexByIdPort) {
    super(ErdOperationType.CHANGE_INDEX_NAME, ChangeIndexNameInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeIndexNamePort = changeIndexNamePort;
    this.getIndexByIdPort = getIndexByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeIndexNameInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getIndexByIdPort.findIndexById(inversePayload.indexId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.NOT_FOUND,
            "Index not found: " + inversePayload.indexId())))
        .flatMap(index -> coordinate(resolved, inversePayload,
            () -> changeIndexNamePort.changeIndexName(inversePayload.indexId(), inversePayload.oldName())
                .thenReturn(MutationResult.<Void>of(null, index.tableId())
                    .withInverse(new ChangeIndexNameInverse(index.id(), index.name())))));
  }

}
