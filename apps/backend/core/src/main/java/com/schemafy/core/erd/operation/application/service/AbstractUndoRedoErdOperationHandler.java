package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.util.StringUtils;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.application.inverse.InversePayload;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import reactor.core.publisher.Mono;

abstract class AbstractUndoRedoErdOperationHandler<T extends InversePayload>
    implements UndoRedoErdOperationHandler {

  private final ErdOperationType operationType;
  private final Class<T> inverseType;
  private final JsonCodec jsonCodec;
  private final ErdMutationCoordinator erdMutationCoordinator;

  AbstractUndoRedoErdOperationHandler(
      ErdOperationType operationType,
      Class<T> inverseType,
      JsonCodec jsonCodec,
      ErdMutationCoordinator erdMutationCoordinator) {
    this.operationType = Objects.requireNonNull(operationType, "operationType");
    this.inverseType = Objects.requireNonNull(inverseType, "inverseType");
    this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    this.erdMutationCoordinator = Objects.requireNonNull(erdMutationCoordinator, "erdMutationCoordinator");
  }

  @Override
  public final boolean supports(ErdOperationType rootOriginalOpType) {
    return operationType == rootOriginalOpType;
  }

  @Override
  public final Mono<MutationResult<Void>> undo(ResolvedUndoRedoEligibility resolved) {
    return execute(resolved);
  }

  @Override
  public final Mono<MutationResult<Void>> redo(ResolvedUndoRedoEligibility resolved) {
    return execute(resolved);
  }

  protected final ErdOperationType operationType() {
    return operationType;
  }

  protected final Mono<MutationResult<Void>> coordinate(
      ResolvedUndoRedoEligibility resolved,
      T inversePayload,
      Supplier<Mono<MutationResult<Void>>> mutationSupplier) {
    ErdOperationLog executionBase = resolved.executionBaseOperation();
    ErdOperationDerivationKind derivationKind = resolved.action() == UndoRedoAction.UNDO
        ? ErdOperationDerivationKind.UNDO
        : ErdOperationDerivationKind.REDO;
    return erdMutationCoordinator.coordinate(operationType, inversePayload, mutationSupplier)
        .contextWrite(ErdOperationContexts.withDerivation(derivationKind, executionBase.opId())
            .andThen(ErdOperationContexts.withBaseSchemaRevision(resolved.schemaCurrentRevision())));
  }

  protected abstract Mono<MutationResult<Void>> applyInverse(
      T inversePayload,
      ResolvedUndoRedoEligibility resolved);

  private Mono<MutationResult<Void>> execute(ResolvedUndoRedoEligibility resolved) {
    return Mono.defer(() -> {
      T inversePayload = deserialize(resolved.executionBaseOperation());
      return applyInverse(inversePayload, resolved);
    });
  }

  private T deserialize(ErdOperationLog executionBase) {
    String inversePayloadJson = executionBase.inversePayloadJson();
    if (!StringUtils.hasText(inversePayloadJson)) {
      throw new DomainException(
          OperationErrorCode.INVERSE_PAYLOAD_MISSING,
          "Inverse payload is missing for operation: " + executionBase.opId());
    }

    InversePayload payload = jsonCodec.fromPersistedJson(inversePayloadJson,
        InversePayload.class);
    if (!inverseType.isInstance(payload)) {
      throw new IllegalStateException(
          "Unexpected inverse payload type for %s: %s".formatted(operationType, payload.getClass().getName()));
    }
    return inverseType.cast(payload);
  }

}
