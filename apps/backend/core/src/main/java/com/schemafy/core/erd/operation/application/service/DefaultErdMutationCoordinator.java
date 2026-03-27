package com.schemafy.core.erd.operation.application.service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.port.out.SaveSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolver.FinalizedErdMutationTarget;
import com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolver.ResolvedErdMutationTarget;
import com.schemafy.core.erd.operation.domain.*;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
class DefaultErdMutationCoordinator implements ErdMutationCoordinator {

  private static final String SYSTEM_ACTOR_USER_ID = "system";

  private final TransactionalOperator transactionalOperator;
  private final ErdMutationTargetResolver erdMutationTargetResolver;
  private final FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;
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
              .switchIfEmpty(executeMutationAndCommit(
                  operationType,
                  payload,
                  mutationSupplier,
                  resolvedTarget,
                  null,
                  metadata)));
    }).as(transactionalOperator::transactional);
  }

  private Mono<SchemaCollaborationState> preloadSchemaState(
      ErdOperationType operationType,
      ResolvedErdMutationTarget resolvedTarget) {
    if (operationType == ErdOperationType.CREATE_SCHEMA) {
      return Mono.empty();
    }
    return loadOrCreateSchemaState(resolvedTarget.schemaId(), resolvedTarget.projectId());
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
        .switchIfEmpty(saveSchemaCollaborationStatePort
            .save(new SchemaCollaborationState(schemaId, projectId, 0L, null, null, null))
            .onErrorResume(DuplicateKeyException.class,
                ex -> findSchemaCollaborationStatePort.findBySchemaId(schemaId)));
  }

  private <T> Mono<MutationResult<T>> commitOperation(
      ErdOperationType operationType,
      Object payload,
      MutationResult<T> mutationResult,
      ResolvedErdMutationTarget resolvedTarget,
      SchemaCollaborationState preloadedState,
      ErdOperationMetadata metadata) {
    FinalizedErdMutationTarget finalizedTarget = erdMutationTargetResolver.finalizeTarget(
        operationType,
        resolvedTarget,
        mutationResult);

    return resolveSchemaState(operationType, resolvedTarget, finalizedTarget, preloadedState)
        .flatMap(schemaState -> saveSchemaCollaborationStatePort.save(schemaState.incremented())
            .flatMap(updatedState -> appendErdOperationLogPort.append(buildOperationLog(
                operationType,
                payload,
                mutationResult,
                finalizedTarget,
                updatedState,
                metadata)))
            .thenReturn(mutationResult));
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
    List<String> affectedTableIds = mutationResult.affectedTableIds().stream()
        .sorted()
        .toList();

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
        ErdOperationDerivationKind.ORIGINAL,
        null,
        ErdOperationLifecycleState.COMMITTED,
        jsonCodec.serialize(payload),
        null,
        jsonCodec.serialize(finalizedTarget.touchedEntity() == null
            ? List.of()
            : List.of(finalizedTarget.touchedEntity())),
        jsonCodec.serialize(affectedTableIds));
  }

}
