package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class SameCommandUndoRedoStrategy implements UndoRedoErdOperationStrategy {

  private final SameCommandReplayRegistry sameCommandReplayRegistry;

  @Override
  public boolean supports(ErdOperationType opType) {
    return sameCommandReplayRegistry.supports(opType);
  }

  @Override
  public Mono<MutationResult<Void>> undo(ErdOperationLog operationLog) {
    Objects.requireNonNull(operationLog, "operationLog");
    return execute(
        operationLog,
        operationLog.inversePayloadJson(),
        ErdOperationDerivationKind.UNDO,
        "Undo payload is missing for operation: " + operationLog.opId());
  }

  @Override
  public Mono<MutationResult<Void>> redo(ErdOperationLog operationLog) {
    Objects.requireNonNull(operationLog, "operationLog");
    return execute(
        operationLog,
        operationLog.payloadJson(),
        ErdOperationDerivationKind.REDO,
        "Redo payload is missing for operation: " + operationLog.opId());
  }

  private Mono<MutationResult<Void>> execute(
      ErdOperationLog operationLog,
      String payloadJson,
      ErdOperationDerivationKind derivationKind,
      String missingPayloadMessage) {
    if (payloadJson == null || payloadJson.isBlank()) {
      return Mono.error(new IllegalStateException(missingPayloadMessage));
    }

    return sameCommandReplayRegistry.executePersisted(operationLog.opType(), payloadJson)
        .contextWrite(ErdOperationContexts.withDerivedFromOpId(operationLog.opId()))
        .contextWrite(ErdOperationContexts.withDerivationKind(derivationKind));
  }

}
