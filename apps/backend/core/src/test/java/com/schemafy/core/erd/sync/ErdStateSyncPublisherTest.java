package com.schemafy.core.erd.sync;

import java.util.Set;

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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ErdStateSyncPublisherTest {

  private static final Set<String> AFFECTED_TABLE_IDS = Set.of("table-1");
  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "operation-1", "client-operation-1", 7L, ErdOperationDerivationKind.ORIGINAL);
  private static final ResolvedContext CONTEXT = new ResolvedContext("project-1", "schema-1");

  @Mock
  ErdMutationBroadcaster mutationBroadcaster;

  @Mock
  ErdStateSnapshotEnqueuer snapshotEnqueuer;

  ErdStateSyncPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new ErdStateSyncPublisher(mutationBroadcaster, snapshotEnqueuer,
        new ErdStateSnapshotEnqueueProperties());
  }

  @Test
  @DisplayName("committed ERD mutation은 즉시 협업 이벤트와 최신 snapshot enqueue를 함께 수행한다")
  void publishesMutationAndEnqueuesSnapshot() {
    given(mutationBroadcaster.resolveFromTableId("table-1")).willReturn(Mono.just(CONTEXT));
    given(mutationBroadcaster.broadcastWithContext(CONTEXT, AFFECTED_TABLE_IDS, OPERATION))
        .willReturn(Mono.empty());
    given(snapshotEnqueuer.enqueueActive("project-1", "schema-1", 7L)).willReturn(Mono.empty());

    StepVerifier.create(publisher.publishMutation(AFFECTED_TABLE_IDS, OPERATION))
        .verifyComplete();

    then(mutationBroadcaster).should().broadcastWithContext(CONTEXT, AFFECTED_TABLE_IDS, OPERATION);
    then(snapshotEnqueuer).should().enqueueActive("project-1", "schema-1", 7L);
  }

  @Test
  @DisplayName("operation이 없는 no-op mutation은 협업 이벤트나 snapshot job을 만들지 않는다")
  void skipsNoOpMutation() {
    StepVerifier.create(publisher.publishMutation(AFFECTED_TABLE_IDS, null))
        .verifyComplete();

    then(mutationBroadcaster).shouldHaveNoInteractions();
    then(snapshotEnqueuer).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("snapshot enqueue 실패는 이미 완료된 mutation 응답을 실패시키지 않는다")
  void suppressesSnapshotEnqueueFailure() {
    given(mutationBroadcaster.resolveFromTableId("table-1")).willReturn(Mono.just(CONTEXT));
    given(mutationBroadcaster.broadcastWithContext(CONTEXT, AFFECTED_TABLE_IDS, OPERATION))
        .willReturn(Mono.empty());
    given(snapshotEnqueuer.enqueueActive("project-1", "schema-1", 7L))
        .willReturn(Mono.error(new IllegalStateException("Redis unavailable")));

    StepVerifier.create(publisher.publishMutation(AFFECTED_TABLE_IDS, OPERATION))
        .verifyComplete();

    then(mutationBroadcaster).should().broadcastWithContext(CONTEXT, AFFECTED_TABLE_IDS, OPERATION);
    then(snapshotEnqueuer).should(times(3)).enqueueActive("project-1", "schema-1", 7L);
  }

}
