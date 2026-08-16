package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.service.SchemaSnapshotOrchestrator;
import com.schemafy.api.erd.service.SchemaStateSnapshot;
import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.collaboration.dto.event.ErdStateChangedEvent;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.common.json.JsonCodec;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErdStateSnapshotWorkerTest {

  @Mock
  private ErdStateSnapshotJobStore jobStore;
  @Mock
  private SchemaSnapshotOrchestrator snapshotOrchestrator;
  @Mock
  private CollaborationEventPublisher eventPublisher;

  private final ErdStateSnapshotProperties properties = properties();
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final Scheduler scheduler = Schedulers.newSingle("snapshot-worker-test");
  private final JsonCodec jsonCodec = new JsonCodec(
      new ObjectMapper().findAndRegisterModules());

  private ErdStateSnapshotWorker worker;

  @BeforeEach
  void setUp() {
    worker = new ErdStateSnapshotWorker(jobStore, snapshotOrchestrator,
        jsonCodec, eventPublisher, meterRegistry, properties, scheduler,
        () -> 2_000L, () -> "lease-token");
  }

  @AfterEach
  void tearDown() {
    scheduler.dispose();
  }

  @Test
  void buildsValidatesAndPublishesAnActiveSnapshot() {
    ErdStateSnapshotJob job = activeJob(10L, 0);
    SchemaStateSnapshot snapshot = snapshot(12L);
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(snapshot));
    given(jobStore.isPublishable(job, 12L)).willReturn(Mono.just(true));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());
    given(jobStore.complete(job, 12L, 2_000L)).willReturn(Mono.empty());

    worker.process("job-1").block();

    ArgumentCaptor<CollaborationOutbound> eventCaptor = ArgumentCaptor.forClass(CollaborationOutbound.class);
    verify(eventPublisher).publishStrict(eq("project-1"), eventCaptor.capture());
    ErdStateChangedEvent.Outbound event = (ErdStateChangedEvent.Outbound) eventCaptor.getValue();
    assertThat(event.state()).isEqualTo(ErdStateChangedEvent.State.ACTIVE);
    assertThat(event.revision()).isEqualTo(12L);
    assertThat(event.schema().get("id").asText()).isEqualTo("schema-1");
    verify(jobStore).complete(job, 12L, 2_000L);
  }

  @Test
  void publishesADeletionWithoutBuildingASnapshot() {
    ErdStateSnapshotJob job = deletedJob(11L);
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(jobStore.isPublishable(job, 11L)).willReturn(Mono.just(true));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());
    given(jobStore.complete(job, 11L, 2_000L)).willReturn(Mono.empty());

    worker.process("job-1").block();

    verify(snapshotOrchestrator, never()).getSchemaState(any());
    ArgumentCaptor<CollaborationOutbound> eventCaptor = ArgumentCaptor.forClass(CollaborationOutbound.class);
    verify(eventPublisher).publishStrict(eq("project-1"), eventCaptor.capture());
    ErdStateChangedEvent.Outbound event = (ErdStateChangedEvent.Outbound) eventCaptor.getValue();
    assertThat(event.state()).isEqualTo(ErdStateChangedEvent.State.DELETED);
    assertThat(event.schema()).isNull();
    verify(jobStore).complete(job, 11L, 2_000L);
  }

  @Test
  void requeuesWithoutPublishingWhenTheCandidateWasSuperseded() {
    ErdStateSnapshotJob job = activeJob(10L, 0);
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(snapshot(10L)));
    given(jobStore.isPublishable(job, 10L)).willReturn(Mono.just(false));
    given(jobStore.requeue(job, 2_000L, Duration.ZERO, false)).willReturn(Mono.empty());

    worker.process("job-1").block();

    verify(eventPublisher, never()).publishStrict(any(), any());
    verify(jobStore, never()).complete(any(), eq(10L), eq(2_000L));
    verify(jobStore).requeue(job, 2_000L, Duration.ZERO, false);
  }

  @Test
  void retriesAnExhaustedBuildThenRequeuesWithDistributedBackoff() {
    ErdStateSnapshotJob job = activeJob(10L, 2);
    AtomicInteger attempts = new AtomicInteger();
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.defer(() -> {
          attempts.incrementAndGet();
          return Mono.error(new IllegalStateException("database unavailable"));
        }));
    given(jobStore.requeue(job, 2_000L, Duration.ofSeconds(4), true))
        .willReturn(Mono.empty());

    worker.process("job-1").block();

    assertThat(attempts).hasValue(4);
    verify(jobStore).requeue(job, 2_000L, Duration.ofSeconds(4), true);
    assertThat(meterRegistry.counter(
        "schemafy.erd.state_snapshot.retry_exhausted", "phase", "build").count())
        .isEqualTo(1D);
  }

  @Test
  void renewsTheLeaseWhileABuildIsStillRunning() {
    ErdStateSnapshotJob job = activeJob(10L, 0);
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.delay(Duration.ofMillis(35), scheduler)
            .thenReturn(snapshot(10L)));
    given(jobStore.renewLease(job, 2_000L, properties.getLeaseTtl()))
        .willReturn(Mono.just(true));
    given(jobStore.isPublishable(job, 10L)).willReturn(Mono.just(true));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());
    given(jobStore.complete(job, 10L, 2_000L)).willReturn(Mono.empty());

    worker.process("job-1").block();

    verify(jobStore, atLeast(2)).renewLease(job, 2_000L,
        properties.getLeaseTtl());
  }

  @Test
  void retriesPublishWithoutRebuildingTheSnapshot() {
    ErdStateSnapshotJob job = activeJob(10L, 0);
    AtomicInteger publishAttempts = new AtomicInteger();
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(snapshot(10L)));
    given(jobStore.isPublishable(job, 10L)).willReturn(Mono.just(true));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.defer(() -> publishAttempts.incrementAndGet() < 3
            ? Mono.error(new IllegalStateException("Redis publish failed"))
            : Mono.empty()));
    given(jobStore.complete(job, 10L, 2_000L)).willReturn(Mono.empty());

    worker.process("job-1").block();

    assertThat(publishAttempts).hasValue(3);
    verify(snapshotOrchestrator, times(1)).getSchemaState("schema-1");
    verify(jobStore).complete(job, 10L, 2_000L);
  }

  @Test
  void revalidatesBeforeEachPublishRetry() {
    ErdStateSnapshotJob job = activeJob(10L, 0);
    AtomicInteger validationAttempts = new AtomicInteger();
    AtomicInteger publishAttempts = new AtomicInteger();
    given(jobStore.claim("job-1", "lease-token", 2_000L,
        properties.getLeaseTtl())).willReturn(Mono.just(job));
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(snapshot(10L)));
    given(jobStore.isPublishable(job, 10L)).willAnswer(ignored -> Mono.just(
        validationAttempts.incrementAndGet() == 1));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.defer(() -> {
          publishAttempts.incrementAndGet();
          return Mono.error(new IllegalStateException("Redis publish failed"));
        }));
    given(jobStore.requeue(job, 2_000L, Duration.ZERO, false)).willReturn(Mono.empty());

    worker.process("job-1").block();

    assertThat(validationAttempts).hasValue(2);
    assertThat(publishAttempts).hasValue(1);
    verify(jobStore, never()).complete(any(), anyLong(), anyLong());
    verify(jobStore).requeue(job, 2_000L, Duration.ZERO, false);
  }

  private ErdStateSnapshotJob activeJob(long revision, int failureCount) {
    return new ErdStateSnapshotJob("job-1", "project-1", "schema-1",
        ErdStateSnapshotJobKind.ACTIVE, revision, 0L, "lease-token",
        failureCount);
  }

  private ErdStateSnapshotJob deletedJob(long revision) {
    return new ErdStateSnapshotJob("job-1", "project-1", "schema-1",
        ErdStateSnapshotJobKind.DELETED, revision, 1L, "lease-token", 0);
  }

  private SchemaStateSnapshot snapshot(long revision) {
    return new SchemaStateSnapshot(
        new SchemaResponse("schema-1", "project-1", "schema", "utf8mb4",
            "utf8mb4_general_ci", null),
        revision, Map.of());
  }

  private static ErdStateSnapshotProperties properties() {
    ErdStateSnapshotProperties properties = new ErdStateSnapshotProperties();
    properties.setLeaseTtl(Duration.ofSeconds(30));
    properties.setLeaseRenewInterval(Duration.ofMillis(10));
    properties.setRetryBackoff(Duration.ofMillis(1));
    properties.setMaxRetryBackoff(Duration.ofMillis(4));
    properties.setRequeueBackoff(Duration.ofSeconds(1));
    properties.setMaxRequeueBackoff(Duration.ofSeconds(30));
    return properties;
  }

}
