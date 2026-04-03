package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.application.port.out.SaveSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolver.FinalizedErdMutationTarget;
import com.schemafy.core.erd.operation.application.service.ErdMutationTargetResolver.ResolvedErdMutationTarget;
import com.schemafy.core.erd.operation.domain.*;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class DefaultErdMutationCoordinator implements ErdMutationCoordinator {

  private static final String SYSTEM_ACTOR_USER_ID = "system";

  private final TransactionalOperator transactionalOperator;
  private final ErdMutationTargetResolver erdMutationTargetResolver;
  private final FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;
  private final IncrementSchemaCollaborationRevisionPort incrementSchemaCollaborationRevisionPort;
  private final SaveSchemaCollaborationStatePort saveSchemaCollaborationStatePort;
  private final AppendErdOperationLogPort appendErdOperationLogPort;
  private final ErdOperationInversePayloadResolver erdOperationInversePayloadResolver;
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
          .flatMap(resolvedTarget -> erdOperationInversePayloadResolver.resolveBefore(operationType, payload)
              .map(jsonCodec::serialize)
              .defaultIfEmpty("")
              .flatMap(inversePayloadJson -> {
                String resolvedInverseJson = inversePayloadJson.isEmpty() ? null : inversePayloadJson;
                return preloadSchemaState(operationType, resolvedTarget)
                    .flatMap(preloadedState -> executeMutationAndCommit(
                        operationType,
                        payload,
                        resolvedInverseJson,
                        mutationSupplier,
                        resolvedTarget,
                        preloadedState,
                        metadata))
                    .switchIfEmpty(Mono.defer(() -> executeMutationAndCommit(
                        operationType,
                        payload,
                        resolvedInverseJson,
                        mutationSupplier,
                        resolvedTarget,
                        null,
                        metadata)));
              }));
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
      String inversePayloadJson,
      Supplier<Mono<MutationResult<T>>> mutationSupplier,
      ResolvedErdMutationTarget resolvedTarget,
      SchemaCollaborationState preloadedState,
      ErdOperationMetadata metadata) {
    return mutationSupplier.get()
        .flatMap(mutationResult -> commitOperation(
            operationType,
            payload,
            inversePayloadJson,
            mutationResult,
            resolvedTarget,
            preloadedState,
            metadata));
  }

  private Mono<SchemaCollaborationState> loadOrCreateSchemaState(String schemaId, String projectId) {
    return findSchemaCollaborationStatePort.findBySchemaId(schemaId)
        .switchIfEmpty(saveSchemaCollaborationStatePort
            .save(new SchemaCollaborationState(schemaId, projectId, 0L, null, null))
            .onErrorResume(DuplicateKeyException.class,
                ex -> findSchemaCollaborationStatePort.findBySchemaId(schemaId)));
  }

  private <T> Mono<MutationResult<T>> commitOperation(
      ErdOperationType operationType,
      Object payload,
      String inversePayloadJson,
      MutationResult<T> mutationResult,
      ResolvedErdMutationTarget resolvedTarget,
      SchemaCollaborationState preloadedState,
      ErdOperationMetadata metadata) {
    FinalizedErdMutationTarget finalizedTarget = erdMutationTargetResolver.finalizeTarget(
        operationType,
        resolvedTarget,
        mutationResult);

    return resolveSchemaState(operationType, resolvedTarget, finalizedTarget, preloadedState)
        .flatMap(schemaState -> incrementSchemaCollaborationRevisionPort.increment(schemaState.schemaId())
            .flatMap(updatedState -> appendErdOperationLogPort.append(buildOperationLog(
                operationType,
                payload,
                inversePayloadJson,
                mutationResult,
                finalizedTarget,
                updatedState,
                metadata)))
            .map(operationLog -> mutationResult.withOperation(
                CommittedErdOperation.from(operationLog))));
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
      String inversePayloadJson,
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
        metadata.derivationKindOr(ErdOperationDerivationKind.ORIGINAL),
        metadata.derivedFromOpId(),
        ErdOperationLifecycleState.COMMITTED,
        jsonCodec.serialize(payload),
        inversePayloadJson,
        jsonCodec.serialize(finalizedTarget.touchedEntity() == null
            ? List.of()
            : List.of(finalizedTarget.touchedEntity())),
        jsonCodec.serialize(affectedTableIds));
  }

}
