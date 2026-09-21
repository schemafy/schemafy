package com.schemafy.api.erd.service.sync;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.erd.broadcast.ErdMutationBroadcaster.ResolvedContext;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdStateSyncPublisher")
class ErdStateSyncPublisherTest {

  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "op-1", "client-op-1", 42L, ErdOperationDerivationKind.ORIGINAL);
  private static final ResolvedContext CONTEXT = new ResolvedContext(
      "project-1", "schema-1");

  @Mock
  private ErdMutationBroadcaster mutationBroadcaster;

  @Mock
  private ErdStateSnapshotProducer snapshotProducer;

  private ErdStateSyncPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new ErdStateSyncPublisher(mutationBroadcaster,
        snapshotProducer);
  }

  @Test
  @DisplayName("일반 mutation은 table context를 한 번 resolve해 두 event 경로를 실행한다")
  void publishesMutationWithResolvedTableContext() {
    Set<String> tableIds = Set.of("table-1");
    given(mutationBroadcaster.resolveFromTableId("table-1"))
        .willReturn(Mono.just(CONTEXT));
    given(mutationBroadcaster.broadcastWithContext(CONTEXT, tableIds,
        OPERATION)).willReturn(Mono.empty());
    given(snapshotProducer.enqueueActive("project-1", "schema-1", 42L))
        .willReturn(Mono.empty());

    StepVerifier.create(publisher.publishMutation(tableIds, OPERATION))
        .verifyComplete();

    then(mutationBroadcaster).should().resolveFromTableId("table-1");
    then(mutationBroadcaster).should()
        .broadcastWithContext(CONTEXT, tableIds, OPERATION);
    then(snapshotProducer).should()
        .enqueueActive("project-1", "schema-1", 42L);
  }

  @Test
  @DisplayName("schema mutation은 schema context와 빈 affected table set을 보존한다")
  void publishesSchemaChange() {
    given(mutationBroadcaster.resolveFromSchemaId("schema-1"))
        .willReturn(Mono.just(CONTEXT));
    given(mutationBroadcaster.broadcastWithContext(CONTEXT, Set.of(),
        OPERATION)).willReturn(Mono.empty());
    given(snapshotProducer.enqueueActive("project-1", "schema-1", 42L))
        .willReturn(Mono.empty());

    StepVerifier.create(publisher.publishSchemaChange("schema-1", OPERATION))
        .verifyComplete();

    then(mutationBroadcaster).should()
        .broadcastWithContext(CONTEXT, Set.of(), OPERATION);
    then(snapshotProducer).should()
        .enqueueActive("project-1", "schema-1", 42L);
  }

  @Test
  @DisplayName("삭제 전 context로 compatibility event와 DELETED tombstone을 발행한다")
  void publishesDeletedWithResolvedContext() {
    Set<String> tableIds = Set.of("table-1");
    given(mutationBroadcaster.broadcastWithContext(CONTEXT, tableIds,
        OPERATION)).willReturn(Mono.empty());
    given(snapshotProducer.enqueueDeleted("project-1", "schema-1", 42L))
        .willReturn(Mono.empty());

    StepVerifier.create(publisher.publishDeletedWithContext(CONTEXT,
        tableIds, OPERATION))
        .verifyComplete();

    then(snapshotProducer).should()
        .enqueueDeleted("project-1", "schema-1", 42L);
  }

  @Test
  @DisplayName("operation이 없으면 event 경로를 실행하지 않는다")
  void skipsMissingOperation() {
    StepVerifier.create(publisher.publishMutation(Set.of("table-1"), null))
        .verifyComplete();

    then(mutationBroadcaster).shouldHaveNoInteractions();
    then(snapshotProducer).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("compatibility event 실패가 state enqueue를 막지 않는다")
  void compatibilityFailureDoesNotBlockStateEnqueue() {
    given(mutationBroadcaster.broadcastWithContext(CONTEXT,
        Set.of("table-1"), OPERATION))
        .willReturn(Mono.error(new IllegalStateException("legacy failed")));
    given(snapshotProducer.enqueueActive("project-1", "schema-1", 42L))
        .willReturn(Mono.empty());

    StepVerifier.create(publisher.publishActiveWithContext(CONTEXT,
        Set.of("table-1"), OPERATION))
        .verifyComplete();

    then(snapshotProducer).should()
        .enqueueActive("project-1", "schema-1", 42L);
  }

  @Test
  @DisplayName("state enqueue 실패가 compatibility event 실행을 막지 않는다")
  void stateEnqueueFailureDoesNotBlockCompatibilityEvent() {
    AtomicBoolean compatibilitySubscribed = new AtomicBoolean();
    given(mutationBroadcaster.broadcastWithContext(CONTEXT,
        Set.of("table-1"), OPERATION))
        .willReturn(Mono.fromRunnable(() -> compatibilitySubscribed.set(true)));
    given(snapshotProducer.enqueueActive("project-1", "schema-1", 42L))
        .willReturn(Mono.error(
            new IllegalStateException("enqueue failed")));

    StepVerifier.create(publisher.publishActiveWithContext(CONTEXT,
        Set.of("table-1"), OPERATION))
        .verifyComplete();

    assertThat(compatibilitySubscribed).isTrue();
  }

}
