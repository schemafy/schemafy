package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.api.erd.service.SchemaSnapshotOrchestrator;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class ErdStateSnapshotWorker {

  private static final long MAX_RETRIES = 3L;

  private final ErdStateSnapshotJobStore jobStore;
  private final SchemaSnapshotOrchestrator snapshotOrchestrator;
  private final JsonCodec jsonCodec;
  private final CollaborationEventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;
  private final ErdStateSnapshotProperties properties;
  private final Scheduler scheduler;
  private final LongSupplier currentTimeMillis;
  private final Supplier<String> leaseTokenSupplier;

  @Autowired
  public ErdStateSnapshotWorker(
      ErdStateSnapshotJobStore jobStore,
      SchemaSnapshotOrchestrator snapshotOrchestrator,
      JsonCodec jsonCodec,
      CollaborationEventPublisher eventPublisher,
      MeterRegistry meterRegistry,
      ErdStateSnapshotProperties properties) {
    this(jobStore, snapshotOrchestrator, jsonCodec, eventPublisher,
        meterRegistry, properties, Schedulers.parallel(),
        System::currentTimeMillis, () -> UUID.randomUUID().toString());
  }

  ErdStateSnapshotWorker(
      ErdStateSnapshotJobStore jobStore,
      SchemaSnapshotOrchestrator snapshotOrchestrator,
      JsonCodec jsonCodec,
      CollaborationEventPublisher eventPublisher,
      MeterRegistry meterRegistry,
      ErdStateSnapshotProperties properties,
      Scheduler scheduler,
      LongSupplier currentTimeMillis,
      Supplier<String> leaseTokenSupplier) {
    this.jobStore = jobStore;
    this.snapshotOrchestrator = snapshotOrchestrator;
    this.jsonCodec = jsonCodec;
    this.eventPublisher = eventPublisher;
    this.meterRegistry = meterRegistry;
    this.properties = properties;
    this.scheduler = scheduler;
    this.currentTimeMillis = currentTimeMillis;
    this.leaseTokenSupplier = leaseTokenSupplier;
  }

  public Mono<Void> process(String jobKey) {
    return Mono.defer(() -> jobStore.claim(jobKey, leaseTokenSupplier.get(),
        now(), properties.getLeaseTtl()))
        .flatMap(this::processClaimed)
        .onErrorResume(error -> {
          log.warn(
              "[ErdStateSnapshotWorker] claim failed: jobKey={}, error={}",
              jobKey, error.getMessage());
          return Mono.empty();
        });
  }

  private Mono<Void> processClaimed(ErdStateSnapshotJob job) {
    return withLeaseHeartbeat(job,
        candidate(job).flatMap(candidate -> publishIfCurrent(job, candidate)))
        .flatMap(revision -> jobStore.complete(job, revision, now()))
        .onErrorResume(SupersededJobException.class,
            error -> jobStore.requeue(job, now(), Duration.ZERO)
                .onErrorResume(requeueError -> {
                  logRequeueFailure(job, requeueError);
                  return Mono.empty();
                }))
        .onErrorResume(error -> requeueAfterFailure(job, error));
  }

  private Mono<SnapshotCandidate> candidate(ErdStateSnapshotJob job) {
    if (job.kind() == ErdStateSnapshotJobKind.DELETED) {
      return Mono.just(new SnapshotCandidate(job.targetRevision(),
          CollaborationOutboundFactory.erdStateChangedDeleted(job.schemaId(),
              job.targetRevision())));
    }
    return withRetry(snapshotOrchestrator.getSchemaState(job.schemaId()),
        "build", job)
        .map(state -> new SnapshotCandidate(state.revision(),
            CollaborationOutboundFactory.erdStateChangedActive(job.schemaId(),
                state.revision(), jsonCodec.toJsonNode(state.schema()),
                jsonCodec.toJsonNode(state.snapshots()))));
  }

  private Mono<Long> publishIfCurrent(ErdStateSnapshotJob job,
      SnapshotCandidate candidate) {
    Mono<Long> publishAttempt = Mono.defer(
        () -> jobStore.isPublishable(job, candidate.revision())
            .flatMap(publishable -> publishable
                ? eventPublisher.publishStrict(job.projectId(),
                    candidate.event()).thenReturn(candidate.revision())
                : Mono.error(new SupersededJobException())));
    return withRetry(publishAttempt, "publish", job);
  }

  private <T> Mono<T> withLeaseHeartbeat(ErdStateSnapshotJob job,
      Mono<T> action) {
    Mono<T> leaseGuard = Flux.interval(properties.getLeaseRenewInterval(),
        scheduler)
        .concatMap(ignored -> jobStore.renewLease(job, now(),
            properties.getLeaseTtl()))
        .flatMap(renewed -> renewed
            ? Mono.empty()
            : Mono.error(new LeaseLostException()))
        .then(Mono.never());
    return Mono.firstWithSignal(action, leaseGuard);
  }

  private <T> Mono<T> withRetry(Mono<T> source, String phase,
      ErdStateSnapshotJob job) {
    Retry retry = Retry.backoff(MAX_RETRIES, properties.getRetryBackoff())
        .maxBackoff(properties.getMaxRetryBackoff())
        .jitter(0D)
        .scheduler(scheduler)
        .filter(error -> !(error instanceof SupersededJobException))
        .doBeforeRetry(signal -> log.warn(
            "[ErdStateSnapshotWorker] retrying: phase={}, projectId={}, schemaId={}, revision={}, retry={}, error={}",
            phase, job.projectId(), job.schemaId(), job.targetRevision(),
            signal.totalRetries() + 1, signal.failure().getMessage()));
    return source.retryWhen(retry)
        .doOnError(error -> {
          if (!(error instanceof SupersededJobException)) {
            meterRegistry.counter(
                "schemafy.erd.state_snapshot.retry_exhausted", "phase", phase)
                .increment();
          }
        });
  }

  private Mono<Void> requeueAfterFailure(ErdStateSnapshotJob job,
      Throwable error) {
    Duration delay = requeueDelay(job.failureCount());
    log.warn(
        "[ErdStateSnapshotWorker] processing failed; requeueing: projectId={}, schemaId={}, revision={}, delay={}, error={}",
        job.projectId(), job.schemaId(), job.targetRevision(), delay,
        error.getMessage());
    return jobStore.requeue(job, now(), delay)
        .onErrorResume(requeueError -> {
          logRequeueFailure(job, requeueError);
          return Mono.empty();
        });
  }

  private Duration requeueDelay(int failureCount) {
    Duration delay = properties.getRequeueBackoff();
    Duration maximum = properties.getMaxRequeueBackoff();
    for (int attempt = 0; attempt < failureCount; attempt++) {
      if (delay.compareTo(maximum.dividedBy(2)) > 0) {
        return maximum;
      }
      delay = delay.multipliedBy(2);
    }
    return delay.compareTo(maximum) > 0 ? maximum : delay;
  }

  private void logRequeueFailure(ErdStateSnapshotJob job, Throwable error) {
    log.warn(
        "[ErdStateSnapshotWorker] requeue failed: projectId={}, schemaId={}, revision={}, error={}",
        job.projectId(), job.schemaId(), job.targetRevision(),
        error.getMessage());
  }

  private long now() {
    return currentTimeMillis.getAsLong();
  }

  private record SnapshotCandidate(long revision,
      CollaborationOutbound event) {
  }

  private static final class SupersededJobException extends RuntimeException {

    private static final long serialVersionUID = 1L;

  }

  private static final class LeaseLostException extends RuntimeException {

    private static final long serialVersionUID = 1L;

  }

}
