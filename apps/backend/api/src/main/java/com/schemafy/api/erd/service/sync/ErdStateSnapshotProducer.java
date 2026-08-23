package com.schemafy.api.erd.service.sync;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.config.ConditionalOnRedisEnabled;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

/** Enqueues snapshot jobs into Redis. Sits on the mutation response path, so
 * a bounded retry (reusing the worker's retryBackoff/maxRetryBackoff) is
 * used to ride out a transient Redis blip instead of losing the job on the
 * first hiccup; a caller that exhausts retries still gets the failure so
 * ErdStateSyncPublisher can log and swallow it as a last resort. */
@Slf4j
@Service
@ConditionalOnRedisEnabled
public class ErdStateSnapshotProducer {

  private static final long MAX_RETRIES = 2L;

  private final ErdStateSnapshotJobStore jobStore;
  private final ErdStateSnapshotProperties properties;
  private final Scheduler scheduler;

  @Autowired
  public ErdStateSnapshotProducer(ErdStateSnapshotJobStore jobStore,
      ErdStateSnapshotProperties properties) {
    this(jobStore, properties, Schedulers.parallel());
  }

  ErdStateSnapshotProducer(ErdStateSnapshotJobStore jobStore,
      ErdStateSnapshotProperties properties, Scheduler scheduler) {
    this.jobStore = jobStore;
    this.properties = properties;
    this.scheduler = scheduler;
  }

  public Mono<Void> enqueueActive(String projectId, String schemaId,
      long targetRevision) {
    return withRetry("active", Mono.defer(() -> jobStore.enqueueActive(
        projectId, schemaId, targetRevision)));
  }

  public Mono<Void> enqueueDeleted(String projectId, String schemaId,
      long targetRevision) {
    return withRetry("deleted", Mono.defer(() -> jobStore.enqueueDeleted(
        projectId, schemaId, targetRevision)));
  }

  private Mono<Void> withRetry(String kind, Mono<Void> action) {
    Retry retry = Retry.backoff(MAX_RETRIES, properties.getRetryBackoff())
        .maxBackoff(properties.getMaxRetryBackoff())
        .jitter(0D)
        .scheduler(scheduler)
        .doBeforeRetry(signal -> log.warn(
            "[ErdStateSnapshotProducer] retrying enqueue: kind={}, retry={}, error={}",
            kind, signal.totalRetries() + 1, signal.failure().getMessage()));
    return action.retryWhen(retry);
  }

}
