package com.schemafy.api.erd.service.sync;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster.ResolvedContext;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class ErdStateSyncPublisher {

  private final ErdMutationBroadcaster mutationBroadcaster;
  private final ErdStateSnapshotProducer snapshotProducer;

  public ErdStateSyncPublisher(ErdMutationBroadcaster mutationBroadcaster,
      ErdStateSnapshotProducer snapshotProducer) {
    this.mutationBroadcaster = mutationBroadcaster;
    this.snapshotProducer = snapshotProducer;
  }

  public Mono<Void> publishMutation(Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    if (operation == null || affectedTableIds == null
        || affectedTableIds.isEmpty()) {
      return Mono.empty();
    }
    String tableId = affectedTableIds.iterator().next();
    return suppressFailure("mutation", Mono.defer(() -> mutationBroadcaster
        .resolveFromTableId(tableId)
        .flatMap(context -> publishActiveWithContext(context,
            affectedTableIds, operation))));
  }

  public Mono<Void> publishSchemaChange(String schemaId,
      CommittedErdOperation operation) {
    return publishSchemaMutation(schemaId, Set.of(), operation);
  }

  public Mono<Void> publishSchemaMutation(String schemaId,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    if (operation == null) {
      return Mono.empty();
    }
    Set<String> tableIds = affectedTableIds == null ? Set.of()
        : affectedTableIds;
    return suppressFailure("schema mutation", Mono.defer(
        () -> mutationBroadcaster.resolveFromSchemaId(schemaId)
            .flatMap(context -> publishActiveWithContext(context, tableIds,
                operation))));
  }

  public Mono<ResolvedContext> resolveFromSchemaId(String schemaId) {
    return mutationBroadcaster.resolveFromSchemaId(schemaId);
  }

  public Mono<ResolvedContext> resolveFromTableId(String tableId) {
    return mutationBroadcaster.resolveFromTableId(tableId);
  }

  public Mono<Void> publishActiveWithContext(ResolvedContext context,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return publishWithContext(context, affectedTableIds, operation, false);
  }

  public Mono<Void> publishDeletedWithContext(ResolvedContext context,
      Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return publishWithContext(context, affectedTableIds, operation, true);
  }

  private Mono<Void> publishWithContext(ResolvedContext context,
      Set<String> affectedTableIds,
      CommittedErdOperation operation,
      boolean deleted) {
    if (operation == null) {
      return Mono.empty();
    }
    Set<String> tableIds = affectedTableIds == null ? Set.of()
        : affectedTableIds;
    Mono<Void> compatibilityEvent = Mono.defer(() -> mutationBroadcaster
        .broadcastWithContext(context, tableIds, operation));
    Mono<Void> stateEvent = Mono.fromRunnable(() -> {
      if (deleted) {
        snapshotProducer.enqueueDeleted(context.projectId(),
            context.schemaId(), operation.committedRevision());
      } else {
        snapshotProducer.enqueueActive(context.projectId(),
            context.schemaId(), operation.committedRevision());
      }
    });
    return suppressFailure(deleted ? "deleted" : "active",
        Mono.whenDelayError(compatibilityEvent, stateEvent));
  }

  private Mono<Void> suppressFailure(String kind, Mono<Void> action) {
    return action
        .doOnError(error -> log.warn(
            "[ErdStateSyncPublisher] publish failed: kind={}, error={}",
            kind, error.getMessage()))
        .onErrorResume(error -> Mono.empty());
  }

}
