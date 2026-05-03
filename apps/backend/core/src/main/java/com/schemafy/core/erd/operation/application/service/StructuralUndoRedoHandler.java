package com.schemafy.core.erd.operation.application.service;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.application.inverse.InversePayload;
import com.schemafy.core.erd.operation.application.inverse.StructuralOperationInverse;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class StructuralUndoRedoHandler implements UndoRedoErdOperationHandler {

  private static final Set<ErdOperationType> SUPPORTED_TYPES = Set.of(
      ErdOperationType.ADD_CONSTRAINT_COLUMN,
      ErdOperationType.REMOVE_CONSTRAINT_COLUMN,
      ErdOperationType.ADD_INDEX_COLUMN,
      ErdOperationType.REMOVE_INDEX_COLUMN,
      ErdOperationType.ADD_RELATIONSHIP_COLUMN,
      ErdOperationType.REMOVE_RELATIONSHIP_COLUMN);

  private final JsonCodec jsonCodec;
  private final ErdMutationCoordinator erdMutationCoordinator;
  private final StructuralSnapshotService structuralSnapshotService;

  @Override
  public boolean supports(ErdOperationType rootOriginalOpType) {
    return SUPPORTED_TYPES.contains(rootOriginalOpType);
  }

  @Override
  public Mono<MutationResult<Void>> undo(ResolvedUndoRedoEligibility resolved) {
    return execute(resolved);
  }

  @Override
  public Mono<MutationResult<Void>> redo(ResolvedUndoRedoEligibility resolved) {
    return execute(resolved);
  }

  private Mono<MutationResult<Void>> execute(ResolvedUndoRedoEligibility resolved) {
    return Mono.defer(() -> {
      ErdOperationType operationType = resolved.targetRootOriginalOperation().opType();
      StructuralOperationInverse inversePayload = deserialize(resolved.executionBaseOperation());
      StructuralSnapshot targetSnapshot = resolved.action() == UndoRedoAction.UNDO
          ? inversePayload.beforeSnapshot()
          : inversePayload.afterSnapshot();
      ErdOperationDerivationKind derivationKind = resolved.action() == UndoRedoAction.UNDO
          ? ErdOperationDerivationKind.UNDO
          : ErdOperationDerivationKind.REDO;

      return erdMutationCoordinator.coordinate(operationType, inversePayload,
          () -> structuralSnapshotService.reconcileTo(targetSnapshot)
              .thenReturn(MutationResult.<Void>of(null, Set.copyOf(inversePayload.affectedTableIds()))
                  .withInverse((InversePayload) inversePayload)))
          .contextWrite(ErdOperationContexts.withDerivation(derivationKind, resolved.executionBaseOperation().opId())
              .andThen(ErdOperationContexts.withBaseSchemaRevision(resolved.schemaCurrentRevision())));
    });
  }

  private StructuralOperationInverse deserialize(ErdOperationLog executionBase) {
    String inversePayloadJson = executionBase.inversePayloadJson();
    if (!StringUtils.hasText(inversePayloadJson)) {
      throw new DomainException(
          OperationErrorCode.INVERSE_PAYLOAD_MISSING,
          "Inverse payload is missing for operation: " + executionBase.opId());
    }

    InversePayload payload = jsonCodec.parse(
        jsonCodec.normalizePersistedJson(inversePayloadJson), InversePayload.class);
    if (payload instanceof StructuralOperationInverse structuralPayload) {
      return structuralPayload;
    }
    throw new IllegalStateException(
        "Unexpected structural inverse payload type: " + payload.getClass().getName());
  }

}
