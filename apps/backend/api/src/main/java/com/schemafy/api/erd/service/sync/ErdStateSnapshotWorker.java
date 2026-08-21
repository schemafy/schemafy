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
  private static final int MAX_BACKOFF_SHIFT = 40;

  private final ErdStateSnapshotJobStore jobStore;
  private final SchemaSnapshotOrchestrator snapshotOrchestrator;
  private final JsonCodec jsonCodec;
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
      MeterRegistry meterRegistry,
      ErdStateSnapshotProperties properties) {
    this(jobStore, snapshotOrchestrator, jsonCodec,
        meterRegistry, properties, Schedulers.parallel(),
        System::currentTimeMillis, () -> UUID.randomUUID().toString());
  }

  ErdStateSnapshotWorker(
      ErdStateSnapshotJobStore jobStore,
      SchemaSnapshotOrchestrator snapshotOrchestrator,
      JsonCodec jsonCodec,
      MeterRegistry meterRegistry,
      ErdStateSnapshotProperties properties,
      Scheduler scheduler,
      LongSupplier currentTimeMillis,
      Supplier<String> leaseTokenSupplier) {
    this.jobStore = jobStore;
    this.snapshotOrchestrator = snapshotOrchestrator;
    this.jsonCodec = jsonCodec;
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
            error -> safeRequeue(job, Duration.ZERO,
                ErdStateSnapshotRequeueReason.SUPERSEDED))
        .onErrorResume(this::isBenignSupersession,
            error -> logBenignSupersession(job, error))
        .onErrorResume(error -> requeueAfterFailure(job, error));
  }

  private boolean isBenignSupersession(Throwable error) {
    return error instanceof LeaseLostException
        || error instanceof JobTransitionRejectedException;
  }

  private Mono<Void> logBenignSupersession(ErdStateSnapshotJob job,
      Throwable error) {
    log.debug(
        "[ErdStateSnapshotWorker] job superseded by a concurrent update, no requeue needed: projectId={}, schemaId={}, revision={}, error={}",
        job.projectId(), job.schemaId(), job.targetRevision(),
        error.getMessage());
    return Mono.empty();
  }

  private Mono<SnapshotCandidate> candidate(ErdStateSnapshotJob job) {
    if (job.kind() == ErdStateSnapshotJobKind.DELETED) {
      return Mono.just(new SnapshotCandidate(job.targetRevision(),
          CollaborationOutboundFactory.erdStateChangedDeleted(job.schemaId(),
              job.targetRevision())));
    }
    return withRetry(
        snapshotOrchestrator.getSchemaStateForSnapshotWorker(job.schemaId()),
        "build", job)
        .map(state -> new SnapshotCandidate(state.revision(),
            CollaborationOutboundFactory.erdStateChangedActive(job.schemaId(),
                state.revision(), jsonCodec.toJsonNode(state.schema()),
                jsonCodec.toJsonNode(state.snapshots()))));
  }

  private Mono<Long> publishIfCurrent(ErdStateSnapshotJob job,
      SnapshotCandidate candidate) {
    String payload = jsonCodec.toJson(candidate.event());
    Mono<Long> publishAttempt = Mono.defer(
        () -> jobStore.publishIfCurrent(job, candidate.revision(), payload)
            .flatMap(published -> published
                ? Mono.just(candidate.revision())
                : Mono.error(new SupersededJobException(
                    "candidate revision no longer publishable for jobKey=%s: candidateRevision=%d"
                        .formatted(job.jobKey(), candidate.revision())))));
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
            : Mono.error(new LeaseLostException(
                "lease renewal rejected for jobKey=%s: lease/generation no longer matches"
                    .formatted(job.jobKey()))))
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
    return safeRequeue(job, delay, ErdStateSnapshotRequeueReason.FAILURE);
  }

  private Mono<Void> safeRequeue(ErdStateSnapshotJob job, Duration delay,
      ErdStateSnapshotRequeueReason reason) {
    return jobStore.requeue(job, now(), delay, reason)
        .onErrorResume(JobTransitionRejectedException.class,
            error -> logBenignSupersession(job, error))
        .onErrorResume(error -> {
          logRequeueFailure(job, error);
          return Mono.empty();
        });
  }

  private Duration requeueDelay(int failureCount) {
    Duration base = properties.getRequeueBackoff();
    Duration maximum = properties.getMaxRequeueBackoff();
    int shift = Math.min(failureCount, MAX_BACKOFF_SHIFT);
    long delayMillis = base.toMillis() << shift;
    if (delayMillis < 0 || delayMillis > maximum.toMillis()) {
      return maximum;
    }
    return Duration.ofMillis(delayMillis);
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

  /** Thrown when {@code publishIfCurrent} reports that a build's candidate
   * revision is no longer current (a newer target, or a delete, already
   * superseded it). This always means a concurrent enqueue call already
   * moved this job past the revision this build was for. Because the
   * validity check and the Redis PUBLISH happen inside the same Lua
   * script, this is also the only way a stale candidate can ever reach
   * subscribers: either both happen atomically, or neither does. */
  private static final class SupersededJobException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    SupersededJobException(String message) {
      super(message);
    }

  }

  /** Thrown when a lease-renewal heartbeat is rejected because the lease
   * token or generation no longer matches the job's current state in
   * Redis. This always means another worker (or a delete/revival event)
   * already took over the job. */
  private static final class LeaseLostException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    LeaseLostException(String message) {
      super(message);
    }

  }

}
