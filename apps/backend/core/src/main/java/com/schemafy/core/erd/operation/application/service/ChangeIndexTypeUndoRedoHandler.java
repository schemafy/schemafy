package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexTypePort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.service.IndexCapabilityResolver;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX;

@Component
class ChangeIndexTypeUndoRedoHandler
    extends AbstractUndoRedoErdOperationHandler<ChangeIndexTypeInverse> {

  private final ChangeIndexTypePort changeIndexTypePort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final IndexCapabilityResolver indexCapabilityResolver;

  ChangeIndexTypeUndoRedoHandler(
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator,
      ChangeIndexTypePort changeIndexTypePort,
      GetIndexByIdPort getIndexByIdPort,
      IndexCapabilityResolver indexCapabilityResolver) {
    super(ErdOperationType.CHANGE_INDEX_TYPE, ChangeIndexTypeInverse.class, jsonCodec, erdMutationCoordinator);
    this.changeIndexTypePort = changeIndexTypePort;
    this.getIndexByIdPort = getIndexByIdPort;
    this.indexCapabilityResolver = indexCapabilityResolver;
  }

  @Override
  protected Mono<MutationResult<Void>> applyInverse(
      ChangeIndexTypeInverse inversePayload,
      ResolvedUndoRedoEligibility resolved) {
    return getIndexByIdPort.findIndexById(inversePayload.indexId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.NOT_FOUND,
            "Index not found: " + inversePayload.indexId())))
        .flatMap(index -> indexCapabilityResolver.resolve(INDEX, index.id())
            .flatMap(capabilities -> {
              IndexValidator.validateType(capabilities, inversePayload.oldType());
              return coordinate(resolved, inversePayload,
                  () -> changeIndexTypePort.changeIndexType(inversePayload.indexId(), inversePayload.oldType())
                      .thenReturn(MutationResult.<Void>of(null, index.tableId())
                          .withInverse(new ChangeIndexTypeInverse(index.id(), index.type()))));
            }));
  }

}
