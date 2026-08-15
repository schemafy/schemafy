package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.api.erd.service.SchemaSnapshotOrchestrator;
import com.schemafy.core.collaboration.dto.event.CollaborationOutboundFactory;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.common.config.ConditionalOnRedisEnabled;
import com.schemafy.core.common.json.JsonCodec;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
@ConditionalOnRedisEnabled
public class ErdStateSnapshotProducer {

  private static final Duration DEFAULT_DEBOUNCE = Duration.ofMillis(100);
  private static final Duration DEFAULT_MAX_WAIT = Duration.ofMillis(500);
  private static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofMillis(100);

  private final SchemaSnapshotOrchestrator snapshotOrchestrator;
  private final JsonCodec jsonCodec;
  private final CollaborationEventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;
  private final Scheduler scheduler;
  private final Duration debounce;
  private final Duration maxWait;
  private final Duration retryBackoff;
  private final Map<SchemaKey, Slot> slots = new ConcurrentHashMap<>();

  @Autowired
  public ErdStateSnapshotProducer(
      SchemaSnapshotOrchestrator snapshotOrchestrator,
      JsonCodec jsonCodec,
      CollaborationEventPublisher eventPublisher,
      MeterRegistry meterRegistry) {
    this(snapshotOrchestrator, jsonCodec, eventPublisher, meterRegistry,
        Schedulers.parallel(), DEFAULT_DEBOUNCE, DEFAULT_MAX_WAIT,
        DEFAULT_RETRY_BACKOFF);
  }

  ErdStateSnapshotProducer(
      SchemaSnapshotOrchestrator snapshotOrchestrator,
      JsonCodec jsonCodec,
      CollaborationEventPublisher eventPublisher,
      MeterRegistry meterRegistry,
      Scheduler scheduler,
      Duration debounce,
      Duration maxWait,
      Duration retryBackoff) {
    this.snapshotOrchestrator = snapshotOrchestrator;
    this.jsonCodec = jsonCodec;
    this.eventPublisher = eventPublisher;
    this.meterRegistry = meterRegistry;
    this.scheduler = scheduler;
    this.debounce = debounce;
    this.maxWait = maxWait;
    this.retryBackoff = retryBackoff;
  }

  public void enqueueActive(String projectId, String schemaId,
      long targetRevision) {
    SchemaKey key = new SchemaKey(projectId, schemaId);
    Slot slot = slots.computeIfAbsent(key, ignored -> new Slot());
    synchronized (slot) {
      if (targetRevision <= Math.max(slot.deletedRevision,
          slot.publishedRevision)) {
        return;
      }
      slot.targetRevision = Math.max(slot.targetRevision, targetRevision);
      if (slot.building) {
        return;
      }
      long now = scheduler.now(TimeUnit.MILLISECONDS);
      if (slot.firstPendingAtMillis < 0L) {
        slot.firstPendingAtMillis = now;
      }
      long debounceAt = now + debounce.toMillis();
      long maxWaitAt = slot.firstPendingAtMillis + maxWait.toMillis();
      long delay = Math.max(0L, Math.min(debounceAt, maxWaitAt) - now);
      if (slot.scheduled != null) {
        slot.scheduled.dispose();
      }
      slot.scheduled = scheduler.schedule(() -> buildActive(key, slot),
          delay, TimeUnit.MILLISECONDS);
    }
  }

  public void enqueueDeleted(String projectId, String schemaId,
      long deletedRevision) {
    SchemaKey key = new SchemaKey(projectId, schemaId);
    Slot slot = slots.computeIfAbsent(key, ignored -> new Slot());
    long generation;
    synchronized (slot) {
      if (deletedRevision <= slot.deletedRevision) {
        return;
      }
      slot.deletedRevision = deletedRevision;
      slot.generation++;
      generation = slot.generation;
      if (slot.scheduled != null) {
        slot.scheduled.dispose();
        slot.scheduled = null;
      }
      slot.firstPendingAtMillis = -1L;
    }
    withRetry(eventPublisher.publishStrict(key.projectId(),
        CollaborationOutboundFactory.erdStateChangedDeleted(key.schemaId(),
            deletedRevision)), "publish", key, deletedRevision)
        .doOnSuccess(ignored -> {
          synchronized (slot) {
            if (generation == slot.generation) {
              slot.publishedRevision = Math.max(slot.publishedRevision,
                  deletedRevision);
            }
          }
        })
        .subscribe(ignored -> {
        }, error -> log.warn(
            "[ErdStateSnapshotProducer] DELETED publish failed: projectId={}, schemaId={}, revision={}, error={}",
            key.projectId(), key.schemaId(), deletedRevision,
            error.getMessage()));
  }

  private void buildActive(SchemaKey key, Slot slot) {
    long generation;
    long buildTargetRevision;
    synchronized (slot) {
      if (slot.building) {
        return;
      }
      slot.building = true;
      slot.scheduled = null;
      slot.firstPendingAtMillis = -1L;
      generation = slot.generation;
      buildTargetRevision = slot.targetRevision;
    }
    AtomicBoolean stateBuilt = new AtomicBoolean();
    withRetry(snapshotOrchestrator.getSchemaState(key.schemaId()), "build",
        key, buildTargetRevision)
        .flatMap(state -> {
          stateBuilt.set(true);
          synchronized (slot) {
            if (generation != slot.generation
                || state.revision() <= slot.deletedRevision
                || state.revision() <= slot.publishedRevision) {
              return Mono.empty();
            }
          }
          return withRetry(eventPublisher.publishStrict(key.projectId(),
              CollaborationOutboundFactory.erdStateChangedActive(
                  key.schemaId(), state.revision(),
                  jsonCodec.toJsonNode(state.schema()),
                  jsonCodec.toJsonNode(state.snapshots()))), "publish", key,
              state.revision())
              .thenReturn(state.revision());
        })
        .subscribe(revision -> {
          synchronized (slot) {
            slot.publishedRevision = Math.max(slot.publishedRevision,
                revision);
          }
        }, error -> {
          log.warn(
              "[ErdStateSnapshotProducer] ACTIVE publish failed: projectId={}, schemaId={}, error={}",
              key.projectId(), key.schemaId(), error.getMessage());
          finishBuild(key, slot, generation, false);
        }, () -> finishBuild(key, slot, generation, stateBuilt.get()));
  }

  private void finishBuild(SchemaKey key, Slot slot, long generation,
      boolean succeeded) {
    synchronized (slot) {
      slot.building = false;
      if (generation != slot.generation || !succeeded
          || slot.targetRevision <= slot.publishedRevision) {
        return;
      }
      slot.scheduled = scheduler.schedule(() -> buildActive(key, slot));
    }
  }

  private <T> Mono<T> withRetry(Mono<T> source, String phase,
      SchemaKey key, long revision) {
    Retry retry = Retry.backoff(3, retryBackoff)
        .maxBackoff(retryBackoff.multipliedBy(4))
        .jitter(0.0)
        .scheduler(scheduler)
        .doBeforeRetry(signal -> log.warn(
            "[ErdStateSnapshotProducer] retrying: phase={}, projectId={}, schemaId={}, revision={}, retry={}, error={}",
            phase, key.projectId(), key.schemaId(), revision,
            signal.totalRetries() + 1, signal.failure().getMessage()));
    return source.retryWhen(retry)
        .doOnError(error -> meterRegistry.counter(
            "schemafy.erd.state_snapshot.retry_exhausted", "phase", phase)
            .increment());
  }

  private record SchemaKey(String projectId, String schemaId) {
  }

  private static final class Slot {

    private long targetRevision = -1L;
    private long publishedRevision = -1L;
    private long deletedRevision = -1L;
    private long firstPendingAtMillis = -1L;
    private long generation;
    private boolean building;
    private Disposable scheduled;

  }

}
