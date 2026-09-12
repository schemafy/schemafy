package com.schemafy.core.erd.sync;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster.ResolvedContext;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service("coreErdStateSyncPublisher")
@RequiredArgsConstructor
@ConditionalOnRedisEnabled
public class ErdStateSyncPublisher {

  private static final long MAX_RETRIES = 2L;

  private final ErdMutationBroadcaster mutationBroadcaster;
  private final ErdStateSnapshotEnqueuer snapshotEnqueuer;
  private final ErdStateSnapshotEnqueueProperties properties;

  public Mono<Void> publishMutation(Set<String> affectedTableIds, CommittedErdOperation operation) {
    if (operation == null || affectedTableIds == null || affectedTableIds.isEmpty()) {
      return Mono.empty();
    }
    String tableId = affectedTableIds.iterator().next();
    return suppressFailure("mutation", Mono.defer(() -> mutationBroadcaster.resolveFromTableId(tableId)
        .flatMap(context -> publishActiveWithContext(context, affectedTableIds, operation))));
  }

  public Mono<Void> publishSchemaChange(String schemaId, CommittedErdOperation operation) {
    return publishSchemaMutation(schemaId, Set.of(), operation);
  }

  public Mono<Void> publishSchemaMutation(String schemaId, Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    if (operation == null) {
      return Mono.empty();
    }
    Set<String> tableIds = affectedTableIds == null ? Set.of() : affectedTableIds;
    return suppressFailure("schema mutation", Mono.defer(() -> mutationBroadcaster
        .resolveFromSchemaId(schemaId)
        .flatMap(context -> publishActiveWithContext(context, tableIds, operation))));
  }

  public Mono<ResolvedContext> resolveFromSchemaId(String schemaId) {
    return mutationBroadcaster.resolveFromSchemaId(schemaId);
  }

  public Mono<ResolvedContext> resolveFromTableId(String tableId) {
    return mutationBroadcaster.resolveFromTableId(tableId);
  }

  public Mono<Void> publishActiveWithContext(ResolvedContext context, Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return publishWithContext(context, affectedTableIds, operation, false);
  }

  public Mono<Void> publishDeletedWithContext(ResolvedContext context, Set<String> affectedTableIds,
      CommittedErdOperation operation) {
    return publishWithContext(context, affectedTableIds, operation, true);
  }

  private Mono<Void> publishWithContext(ResolvedContext context, Set<String> affectedTableIds,
      CommittedErdOperation operation, boolean deleted) {
    if (operation == null) {
      return Mono.empty();
    }
    Set<String> tableIds = affectedTableIds == null ? Set.of() : affectedTableIds;
    Mono<Void> compatibilityEvent = mutationBroadcaster.broadcastWithContext(context, tableIds, operation);
    Mono<Void> snapshotEvent = deleted
        ? enqueueWithRetry("deleted", snapshotEnqueuer.enqueueDeleted(
            context.projectId(), context.schemaId(), operation.committedRevision()))
        : enqueueWithRetry("active", snapshotEnqueuer.enqueueActive(
            context.projectId(), context.schemaId(), operation.committedRevision()));
    return suppressFailure(deleted ? "deleted" : "active", Mono.whenDelayError(compatibilityEvent, snapshotEvent));
  }

  private Mono<Void> enqueueWithRetry(String kind, Mono<Void> action) {
    return action.retryWhen(Retry.backoff(MAX_RETRIES, properties.getRetryBackoff())
        .maxBackoff(properties.getMaxRetryBackoff())
        .jitter(0D)
        .scheduler(Schedulers.parallel())
        .doBeforeRetry(signal -> log.warn("mcp_erd_snapshot_enqueue_retry kind={} retry={}", kind,
            signal.totalRetries() + 1)));
  }

  private Mono<Void> suppressFailure(String kind, Mono<Void> action) {
    return action.doOnError(error -> log.warn("erd_state_sync_publish_failed kind={}", kind, error))
        .onErrorResume(error -> Mono.empty());
  }

}
