package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import reactor.core.publisher.Sinks;
import reactor.test.scheduler.VirtualTimeScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdStateSnapshotProducer")
class ErdStateSnapshotProducerTest {

  @Mock
  private SchemaSnapshotOrchestrator snapshotOrchestrator;

  @Mock
  private CollaborationEventPublisher eventPublisher;

  private VirtualTimeScheduler scheduler;
  private SimpleMeterRegistry meterRegistry;
  private ErdStateSnapshotProducer producer;

  @BeforeEach
  void setUp() {
    scheduler = VirtualTimeScheduler.create();
    meterRegistry = new SimpleMeterRegistry();
    producer = new ErdStateSnapshotProducer(
        snapshotOrchestrator,
        new JsonCodec(new ObjectMapper().findAndRegisterModules()),
        eventPublisher,
        meterRegistry,
        scheduler,
        Duration.ofMillis(100),
        Duration.ofMillis(500),
        Duration.ofMillis(10));
  }

  @Test
  @DisplayName("같은 schema의 100ms burst는 최신 complete state 한 번으로 합친다")
  void coalescesBurstIntoOneActiveState() {
    SchemaStateSnapshot state = new SchemaStateSnapshot(
        new SchemaResponse("schema-1", "project-1", "service", "utf8mb4",
            "utf8mb4_general_ci", null),
        12L,
        Map.of());
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(state));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    producer.enqueueActive("project-1", "schema-1", 11L);
    producer.enqueueActive("project-1", "schema-1", 12L);

    scheduler.advanceTimeBy(Duration.ofMillis(99));
    then(snapshotOrchestrator).should(never()).getSchemaState(any());

    scheduler.advanceTimeBy(Duration.ofMillis(1));

    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");
    ArgumentCaptor<CollaborationOutbound> eventCaptor = ArgumentCaptor
        .forClass(CollaborationOutbound.class);
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), eventCaptor.capture());
    ErdStateChangedEvent.Outbound event = (ErdStateChangedEvent.Outbound) eventCaptor
        .getValue();
    assertThat(event.state()).isEqualTo(ErdStateChangedEvent.State.ACTIVE);
    assertThat(event.schemaId()).isEqualTo("schema-1");
    assertThat(event.revision()).isEqualTo(12L);
    assertThat(event.schema().path("name").asText()).isEqualTo("service");
    assertThat(event.snapshots().isObject()).isTrue();
  }

  @Test
  @DisplayName("지속 mutation에서도 최초 enqueue 후 500ms에 build를 시작한다")
  void startsBuildAtMaxWaitDuringContinuousMutations() {
    SchemaStateSnapshot state = new SchemaStateSnapshot(
        new SchemaResponse("schema-1", "project-1", "service", "utf8mb4",
            "utf8mb4_general_ci", null),
        6L,
        Map.of());
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(state));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    for (long revision = 1L; revision <= 5L; revision++) {
      producer.enqueueActive("project-1", "schema-1", revision);
      scheduler.advanceTimeBy(Duration.ofMillis(90));
    }
    producer.enqueueActive("project-1", "schema-1", 6L);

    scheduler.advanceTimeBy(Duration.ofMillis(49));
    then(snapshotOrchestrator).should(never()).getSchemaState(any());

    scheduler.advanceTimeBy(Duration.ofMillis(1));
    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");
  }

  @Test
  @DisplayName("build 중 더 높은 revision은 겹치지 않고 완료 후 다음 build를 실행한다")
  void higherRevisionDuringBuildRunsAfterCurrentBuild() {
    Sinks.One<SchemaStateSnapshot> firstBuild = Sinks.one();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(firstBuild.asMono(), Mono.just(stateAtRevision(11L)));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");

    producer.enqueueActive("project-1", "schema-1", 11L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");

    firstBuild.tryEmitValue(stateAtRevision(10L));
    scheduler.advanceTimeBy(Duration.ZERO);

    then(snapshotOrchestrator).should(times(2)).getSchemaState("schema-1");
  }

  @Test
  @DisplayName("DELETED는 예약된 ACTIVE build를 취소하고 tombstone을 즉시 발행한다")
  void deletedCancelsScheduledActiveBuild() {
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    producer.enqueueDeleted("project-1", "schema-1", 11L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));

    then(snapshotOrchestrator).shouldHaveNoInteractions();
    ArgumentCaptor<CollaborationOutbound> eventCaptor = ArgumentCaptor
        .forClass(CollaborationOutbound.class);
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), eventCaptor.capture());
    ErdStateChangedEvent.Outbound tombstone = (ErdStateChangedEvent.Outbound) eventCaptor
        .getValue();
    assertThat(tombstone.state())
        .isEqualTo(ErdStateChangedEvent.State.DELETED);
    assertThat(tombstone.revision()).isEqualTo(11L);
    assertThat(tombstone.schema()).isNull();
    assertThat(tombstone.snapshots()).isNull();
  }

  @Test
  @DisplayName("DELETED 이후 완료된 이전 ACTIVE build 결과는 발행하지 않는다")
  void deletedInvalidatesInFlightActiveBuild() {
    Sinks.One<SchemaStateSnapshot> activeBuild = Sinks.one();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(activeBuild.asMono());
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    producer.enqueueDeleted("project-1", "schema-1", 11L);

    activeBuild.tryEmitValue(stateAtRevision(10L));
    scheduler.advanceTimeBy(Duration.ZERO);

    ArgumentCaptor<CollaborationOutbound> eventCaptor = ArgumentCaptor
        .forClass(CollaborationOutbound.class);
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), eventCaptor.capture());
    ErdStateChangedEvent.Outbound event = (ErdStateChangedEvent.Outbound) eventCaptor
        .getValue();
    assertThat(event.state()).isEqualTo(ErdStateChangedEvent.State.DELETED);
  }

  @Test
  @DisplayName("build 실패는 3회 재시도한 뒤 complete state만 발행한다")
  void retriesBuildThreeTimes() {
    AtomicInteger buildAttempts = new AtomicInteger();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.defer(() -> buildAttempts.incrementAndGet() < 4
            ? Mono.error(new IllegalStateException("build failed"))
            : Mono.just(stateAtRevision(10L))));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    scheduler.advanceTimeBy(Duration.ofMillis(70));

    assertThat(buildAttempts).hasValue(4);
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), any());
  }

  @Test
  @DisplayName("publish 실패 재시도는 complete state를 다시 build하지 않는다")
  void retriesPublishWithoutRebuilding() {
    AtomicInteger publishAttempts = new AtomicInteger();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(stateAtRevision(10L)));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.defer(() -> publishAttempts.incrementAndGet() < 4
            ? Mono.error(new IllegalStateException("publish failed"))
            : Mono.empty()));

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    scheduler.advanceTimeBy(Duration.ofMillis(70));

    assertThat(publishAttempts).hasValue(4);
    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");
  }

  @Test
  @DisplayName("build 재시도 소진은 phase metric을 남기고 종료한다")
  void recordsMetricWhenBuildRetriesExhausted() {
    AtomicInteger buildAttempts = new AtomicInteger();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.defer(() -> {
          buildAttempts.incrementAndGet();
          return Mono.error(new IllegalStateException("build failed"));
        }));

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    scheduler.advanceTimeBy(Duration.ofMillis(70));

    assertThat(buildAttempts).hasValue(4);
    assertThat(meterRegistry
        .get("schemafy.erd.state_snapshot.retry_exhausted")
        .tag("phase", "build")
        .counter()
        .count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("이미 발행한 revision 이하 target은 다시 build하지 않는다")
  void skipsTargetAtOrBelowPublishedRevision() {
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(stateAtRevision(12L)));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    producer.enqueueActive("project-1", "schema-1", 11L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));

    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), any());
  }

  @Test
  @DisplayName("이미 발행한 revision의 stale read는 버리고 더 높은 target build를 유지한다")
  void dropsPublishedRevisionAndKeepsHigherTargetBuild() {
    Sinks.One<SchemaStateSnapshot> nextBuild = Sinks.one();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(stateAtRevision(12L)),
            Mono.just(stateAtRevision(12L)), nextBuild.asMono());
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    producer.enqueueActive("project-1", "schema-1", 13L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    scheduler.advanceTimeBy(Duration.ZERO);

    then(snapshotOrchestrator).should(times(3)).getSchemaState("schema-1");
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-1"), any());
  }

  @Test
  @DisplayName("서로 다른 schema의 build는 독립적으로 실행된다")
  void buildsDifferentSchemasIndependently() {
    Sinks.One<SchemaStateSnapshot> firstSchemaBuild = Sinks.one();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(firstSchemaBuild.asMono());
    given(snapshotOrchestrator.getSchemaState("schema-2"))
        .willReturn(Mono.just(state("project-2", "schema-2", 20L)));
    given(eventPublisher.publishStrict(any(), any()))
        .willReturn(Mono.empty());

    producer.enqueueActive("project-1", "schema-1", 10L);
    producer.enqueueActive("project-2", "schema-2", 20L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));

    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-1");
    then(snapshotOrchestrator).should(times(1)).getSchemaState("schema-2");
    then(eventPublisher).should(times(1))
        .publishStrict(eq("project-2"), any());

    firstSchemaBuild.tryEmitValue(stateAtRevision(10L));
  }

  @Test
  @DisplayName("publish 재시도 소진은 phase metric을 남기고 종료한다")
  void recordsMetricWhenPublishRetriesExhausted() {
    AtomicInteger publishAttempts = new AtomicInteger();
    given(snapshotOrchestrator.getSchemaState("schema-1"))
        .willReturn(Mono.just(stateAtRevision(10L)));
    given(eventPublisher.publishStrict(eq("project-1"), any()))
        .willReturn(Mono.defer(() -> {
          publishAttempts.incrementAndGet();
          return Mono.error(new IllegalStateException("publish failed"));
        }));

    producer.enqueueActive("project-1", "schema-1", 10L);
    scheduler.advanceTimeBy(Duration.ofMillis(100));
    scheduler.advanceTimeBy(Duration.ofMillis(70));

    assertThat(publishAttempts).hasValue(4);
    assertThat(meterRegistry
        .get("schemafy.erd.state_snapshot.retry_exhausted")
        .tag("phase", "publish")
        .counter()
        .count()).isEqualTo(1.0);
  }

  private SchemaStateSnapshot stateAtRevision(long revision) {
    return state("project-1", "schema-1", revision);
  }

  private SchemaStateSnapshot state(String projectId, String schemaId,
      long revision) {
    return new SchemaStateSnapshot(
        new SchemaResponse(schemaId, projectId, "service", "utf8mb4",
            "utf8mb4_general_ci", null),
        revision,
        Map.of());
  }

}
