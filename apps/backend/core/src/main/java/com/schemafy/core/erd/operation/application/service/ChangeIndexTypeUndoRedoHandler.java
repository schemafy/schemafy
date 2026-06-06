package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

@Component
class ChangeIndexTypeUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeIndexTypeInverse> {

  private final ChangeIndexTypePort changeIndexTypePort;
  private final GetIndexByIdPort getIndexByIdPort;

  ChangeIndexTypeUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeIndexTypePort changeIndexTypePort,
      GetIndexByIdPort getIndexByIdPort) {
    super(ErdOperationType.CHANGE_INDEX_TYPE, ChangeIndexTypeInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeIndexTypePort = changeIndexTypePort;
    this.getIndexByIdPort = getIndexByIdPort;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeIndexTypeInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getIndexByIdPort.findIndexById(inversePayload.indexId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.NOT_FOUND,
            "Index not found: " + inversePayload.indexId())))
        .flatMap(index -> coordinate(resolved, inversePayload,
            () -> changeIndexTypePort.changeIndexType(inversePayload.indexId(), inversePayload.oldType())
                .thenReturn(MutationResult.<Void>of(null, index.tableId())
                    .withInverse(new ChangeIndexTypeInverse(index.id(), index.type())))));
  }

}
