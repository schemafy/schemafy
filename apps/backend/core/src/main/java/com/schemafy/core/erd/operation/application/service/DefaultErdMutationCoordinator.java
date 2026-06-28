package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.application.port.out.SaveSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.domain.*;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class DefaultErdMutationCoordinator implements ErdMutationCoordinator {

  private static final String SYSTEM_ACTOR_USER_ID = "system";

  private final TransactionalOperator transactionalOperator;
  private final ErdMutationTargetResolver erdMutationTargetResolver;
  private final ErdMutationTargetFinalizer erdMutationTargetFinalizer;
  private final FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;
  private final IncrementSchemaCollaborationRevisionPort incrementSchemaCollaborationRevisionPort;
  private final SaveSchemaCollaborationStatePort saveSchemaCollaborationStatePort;
  private final AppendErdOperationLogPort appendErdOperationLogPort;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final JsonCodec jsonCodec;

  @Override
  public <T> Mono<MutationResult<T>> coordinate(
      ErdOperationType operationType,
      Object payload,
      Supplier<Mono<MutationResult<T>>> mutationSupplier) {
    Objects.requireNonNull(operationType, "operationType");
    Objects.requireNonNull(payload, "payload");
    Objects.requireNonNull(mutationSupplier, "mutationSupplier");

    return Mono.deferContextual(contextView -> {
      if (ErdOperationContexts.isNestedMutationSuppressed(contextView)) {
        return mutationSupplier.get();
      }
      ErdOperationMetadata metadata = ErdOperationContexts.metadata(contextView);

      return erdMutationTargetResolver.resolveBefore(operationType, payload)
          .flatMap(resolvedTarget -> preloadSchemaState(operationType, resolvedTarget)
              .flatMap(preloadedState -> executeMutationAndCommit(
                  operationType,
                  payload,
                  mutationSupplier,
                  resolvedTarget,
                  preloadedState,
                  metadata))
              .switchIfEmpty(Mono.defer(() -> executeMutationAndCommit(
                  operationType,
                  payload,
                  mutationSupplier,
                  resolvedTarget,
                  null,
                  metadata))));
    }).as(transactionalOperator::transactional);
  }

  private Mono<SchemaCollaborationState> preloadSchemaState(
      ErdOperationType operationType,
      ResolvedErdMutationTarget resolvedTarget) {
    if (operationType == ErdOperationType.CREATE_SCHEMA) {
      return Mono.empty();
    }
    return loadOrCreateSchemaStateForUpdate(resolvedTarget.schemaId(), resolvedTarget.projectId());
  }

  private <T> Mono<MutationResult<T>> executeMutationAndCommit(
      ErdOperationType operationType,
      Object payload,
      Supplier<Mono<MutationResult<T>>> mutationSupplier,
      ResolvedErdMutationTarget resolvedTarget,
      SchemaCollaborationState preloadedState,
      ErdOperationMetadata metadata) {
    return mutationSupplier.get()
        .flatMap(mutationResult -> commitOperation(
            operationType,
            payload,
            mutationResult,
            resolvedTarget,
            preloadedState,
            metadata));
  }

  private Mono<SchemaCollaborationState> loadOrCreateSchemaState(String schemaId, String projectId) {
    return findSchemaCollaborationStatePort.findBySchemaId(schemaId)
        .switchIfEmpty(Mono.defer(() -> saveSchemaCollaborationStatePort
            .save(new SchemaCollaborationState(schemaId, projectId, 0L, null, null))
            .onErrorResume(DuplicateKeyException.class,
                ex -> findSchemaCollaborationStatePort.findBySchemaId(schemaId))));
  }

  private Mono<SchemaCollaborationState> loadOrCreateSchemaStateForUpdate(String schemaId, String projectId) {
    return findSchemaCollaborationStatePort.findBySchemaIdForUpdate(schemaId)
        .switchIfEmpty(Mono.defer(() -> saveSchemaCollaborationStatePort
            .save(new SchemaCollaborationState(schemaId, projectId, 0L, null, null))
            .onErrorResume(DuplicateKeyException.class, ex -> Mono.empty())
            .then(findSchemaCollaborationStatePort.findBySchemaIdForUpdate(schemaId))
            .switchIfEmpty(Mono.error(new IllegalStateException(
                "Schema collaboration state missing after creation: schemaId=" + schemaId)))));
  }

  private <T> Mono<MutationResult<T>> commitOperation(
      ErdOperationType operationType,
      Object payload,
      MutationResult<T> mutationResult,
      ResolvedErdMutationTarget resolvedTarget,
      SchemaCollaborationState preloadedState,
      ErdOperationMetadata metadata) {
    if (mutationResult.noOp()) {
      return Mono.just(mutationResult);
    }

    FinalizedErdMutationTarget finalizedTarget = erdMutationTargetFinalizer.finalizeTarget(
        operationType,
        resolvedTarget,
        mutationResult);

    return resolveSchemaState(operationType, resolvedTarget, finalizedTarget, preloadedState)
        .flatMap(schemaState -> incrementRevision(schemaState.schemaId(), metadata)
            .flatMap(updatedState -> appendErdOperationLogPort.append(buildOperationLog(
                operationType,
                payload,
                mutationResult,
                finalizedTarget,
                updatedState,
                metadata)))
            .map(operationLog -> mutationResult.withOperation(
                CommittedErdOperation.from(operationLog))));
  }

  private Mono<SchemaCollaborationState> incrementRevision(
      String schemaId,
      ErdOperationMetadata metadata) {
    Long expectedRevision = metadata.baseSchemaRevision();
    if (isDerivedMutation(metadata)) {
      if (expectedRevision == null) {
        return Mono.error(new IllegalStateException("Derived operation requires base schema revision"));
      }
      return incrementSchemaCollaborationRevisionPort
          .incrementIfCurrentRevision(schemaId, expectedRevision)
          .switchIfEmpty(Mono.error(new DomainException(
              staleDerivedOperationErrorCode(metadata),
              "Schema revision changed before undo/redo commit: schemaId=" + schemaId)));
    }
    return incrementSchemaCollaborationRevisionPort.increment(schemaId);
  }

  private boolean isDerivedMutation(ErdOperationMetadata metadata) {
    ErdOperationDerivationKind derivationKind = metadata.derivationKind();
    return derivationKind == ErdOperationDerivationKind.UNDO
        || derivationKind == ErdOperationDerivationKind.REDO;
  }

  private OperationErrorCode staleDerivedOperationErrorCode(ErdOperationMetadata metadata) {
    if (metadata.derivationKind() == ErdOperationDerivationKind.REDO) {
      return OperationErrorCode.REDO_NOT_ELIGIBLE;
    }
    return OperationErrorCode.SUPERSEDED;
  }

  private Mono<SchemaCollaborationState> resolveSchemaState(
      ErdOperationType operationType,
      ResolvedErdMutationTarget resolvedTarget,
      FinalizedErdMutationTarget finalizedTarget,
      SchemaCollaborationState preloadedState) {
    if (preloadedState != null) {
      return Mono.just(preloadedState);
    }
    if (operationType != ErdOperationType.CREATE_SCHEMA) {
      return Mono.error(new IllegalStateException("Schema collaboration state must be preloaded"));
    }
    return loadOrCreateSchemaState(finalizedTarget.schemaId(), resolvedTarget.projectId());
  }

  private <T> ErdOperationLog buildOperationLog(
      ErdOperationType operationType,
      Object payload,
      MutationResult<T> mutationResult,
      FinalizedErdMutationTarget finalizedTarget,
      SchemaCollaborationState updatedState,
      ErdOperationMetadata metadata) {
    List<String> affectedTableIds = mutationResult.sortedAffectedTableIds();

    return new ErdOperationLog(
        ulidGeneratorPort.generate(),
        finalizedTarget.projectId(),
        updatedState.schemaId(),
        operationType,
        updatedState.currentRevision(),
        metadata.baseSchemaRevision(),
        metadata.clientOperationId(),
        metadata.sessionId(),
        metadata.actorUserIdOr(SYSTEM_ACTOR_USER_ID),
        metadata.derivationKindOrDefault(),
        metadata.derivedFromOpId(),
        ErdOperationLifecycleState.COMMITTED,
        serializePayload(payload),
        mutationResult.inversePayload() == null
            ? null
            : jsonCodec.toJson(mutationResult.inversePayload()),
        jsonCodec.toJson(affectedTableIds));
  }

  private String serializePayload(Object payload) {
    return jsonCodec.toJson(payload);
  }

}
