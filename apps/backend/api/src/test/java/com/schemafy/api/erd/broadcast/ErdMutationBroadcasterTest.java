package com.schemafy.api.erd.broadcast;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.collaboration.constant.CollaborationConstants;
import com.schemafy.api.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.api.collaboration.dto.event.ErdMutatedEvent;
import com.schemafy.api.collaboration.service.CollaborationEventPublisher;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdMutationBroadcaster 단위 테스트")
class ErdMutationBroadcasterTest {

  private static final CommittedErdOperation OPERATION = new CommittedErdOperation(
      "op-1",
      "client-op-1",
      42L,
      ErdOperationDerivationKind.ORIGINAL);

  @InjectMocks
  private ErdMutationBroadcaster broadcaster;

  @Mock
  private GetTableByIdPort getTableByIdPort;

  @Mock
  private GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  private CollaborationEventPublisher eventPublisher;

  @Nested
  @DisplayName("broadcast")
  class Broadcast {

    @Test
    @DisplayName("sessionId context가 없으면 sessionId 없이 이벤트를 발행한다")
    void publishes_event_without_session_id_when_context_missing() {
      String tableId = "table-1";
      String schemaId = "schema-1";
      String projectId = "project-1";
      Set<String> tableIds = Set.of(tableId);

      given(getTableByIdPort.findTableById(tableId))
          .willReturn(Mono.just(new Table(tableId, schemaId,
              "test_table", "utf8mb4", "utf8mb4_general_ci")));
      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));
      given(eventPublisher.publish(eq(projectId),
          any(CollaborationOutbound.class)))
          .willReturn(Mono.empty());

      StepVerifier.create(broadcaster.broadcast(tableIds, OPERATION))
          .verifyComplete();

      ArgumentCaptor<CollaborationOutbound> captor = ArgumentCaptor
          .forClass(CollaborationOutbound.class);
      verify(eventPublisher).publish(eq(projectId), captor.capture());
      assertThat(captor.getValue()).isInstanceOf(
          ErdMutatedEvent.Outbound.class);
      ErdMutatedEvent.Outbound event = (ErdMutatedEvent.Outbound) captor
          .getValue();
      assertThat(event.sessionId()).isNull();
      assertThat(event.schemaId()).isEqualTo(schemaId);
      assertThat(event.operation()).isEqualTo(OPERATION);
      assertThat(event.affectedTableIds()).containsExactly(tableId);
    }

    @Test
    @DisplayName("reactor context에 sessionId가 있으면 ERD_MUTATED에 포함한다")
    void publishes_event_with_session_id_from_reactor_context() {
      String tableId = "table-1";
      String schemaId = "schema-1";
      String projectId = "project-1";
      Set<String> tableIds = Set.of(tableId);

      given(getTableByIdPort.findTableById(tableId))
          .willReturn(Mono.just(new Table(tableId, schemaId,
              "test_table", "utf8mb4", "utf8mb4_general_ci")));
      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));
      given(eventPublisher.publish(eq(projectId),
          any(CollaborationOutbound.class)))
          .willReturn(Mono.empty());

      StepVerifier.create(broadcaster.broadcast(tableIds, OPERATION)
          .contextWrite(ctx -> ctx.put(
              CollaborationConstants.SESSION_ID_CONTEXT_KEY,
              "session-1")))
          .verifyComplete();

      ArgumentCaptor<CollaborationOutbound> captor = ArgumentCaptor
          .forClass(CollaborationOutbound.class);
      verify(eventPublisher).publish(eq(projectId), captor.capture());
      assertThat(captor.getValue()).isInstanceOf(
          ErdMutatedEvent.Outbound.class);
      ErdMutatedEvent.Outbound event = (ErdMutatedEvent.Outbound) captor
          .getValue();
      assertThat(event.sessionId()).isEqualTo("session-1");
      assertThat(event.schemaId()).isEqualTo(schemaId);
      assertThat(event.operation()).isEqualTo(OPERATION);
      assertThat(event.affectedTableIds()).containsExactly(tableId);
    }

    @Test
    @DisplayName("빈 affectedTableIds이면 skip한다")
    void skips_when_empty_table_ids() {
      StepVerifier.create(broadcaster.broadcast(Set.of(), null))
          .verifyComplete();

      verify(getTableByIdPort, never()).findTableById(any());
    }

    @Test
    @DisplayName("null이면 skip한다")
    void skips_when_null() {
      StepVerifier.create(broadcaster.broadcast(null, null))
          .verifyComplete();

      verify(getTableByIdPort, never()).findTableById(any());
    }

    @Test
    @DisplayName("resolve 실패 시 swallow한다")
    void swallows_resolve_error() {
      String tableId = "table-1";
      Set<String> tableIds = Set.of(tableId);

      given(getTableByIdPort.findTableById(tableId))
          .willReturn(Mono.error(new RuntimeException("DB down")));

      StepVerifier.create(broadcaster.broadcast(tableIds, null))
          .verifyComplete();

      verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("publish 에러 발생 시 swallow한다")
    void swallows_publish_error() {
      String tableId = "table-1";
      String schemaId = "schema-1";
      String projectId = "project-1";
      Set<String> tableIds = Set.of(tableId);

      given(getTableByIdPort.findTableById(tableId))
          .willReturn(Mono.just(new Table(tableId, schemaId,
              "test_table", "utf8mb4", "utf8mb4_general_ci")));
      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));
      given(eventPublisher.publish(eq(projectId),
          any(CollaborationOutbound.class)))
          .willReturn(Mono.error(new RuntimeException("Redis down")));

      StepVerifier.create(broadcaster.broadcast(tableIds, null))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("broadcastSchemaChange")
  class BroadcastSchemaChange {

    @Test
    @DisplayName("정상적으로 이벤트를 발행한다")
    void publishes_event() {
      String schemaId = "schema-1";
      String projectId = "project-1";

      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));
      given(eventPublisher.publish(eq(projectId),
          any(CollaborationOutbound.class)))
          .willReturn(Mono.empty());

      StepVerifier.create(broadcaster.broadcastSchemaChange(schemaId,
          OPERATION))
          .verifyComplete();

      ArgumentCaptor<CollaborationOutbound> captor = ArgumentCaptor
          .forClass(CollaborationOutbound.class);
      verify(eventPublisher).publish(eq(projectId), captor.capture());
      ErdMutatedEvent.Outbound event = (ErdMutatedEvent.Outbound) captor
          .getValue();
      assertThat(event.operation()).isEqualTo(OPERATION);
    }

  }

  @Nested
  @DisplayName("broadcastWithContext")
  class BroadcastWithContext {

    @Test
    @DisplayName("이미 해소된 context로 발행한다")
    void publishes_with_given_context() {
      String projectId = "project-1";
      String schemaId = "schema-1";
      Set<String> tableIds = Set.of("table-1");
      var ctx = new ErdMutationBroadcaster.ResolvedContext(projectId,
          schemaId);

      given(eventPublisher.publish(eq(projectId),
          any(CollaborationOutbound.class)))
          .willReturn(Mono.empty());

      StepVerifier.create(
          broadcaster.broadcastWithContext(ctx, tableIds, OPERATION))
          .verifyComplete();

      ArgumentCaptor<CollaborationOutbound> captor = ArgumentCaptor
          .forClass(CollaborationOutbound.class);
      verify(eventPublisher).publish(eq(projectId), captor.capture());
      ErdMutatedEvent.Outbound event = (ErdMutatedEvent.Outbound) captor
          .getValue();
      assertThat(event.operation()).isEqualTo(OPERATION);
    }

  }

  @Nested
  @DisplayName("resolveFromSchemaId")
  class ResolveFromSchemaId {

    @Test
    @DisplayName("정상적으로 context를 해소한다")
    void resolves() {
      String schemaId = "schema-1";
      String projectId = "project-1";

      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));

      StepVerifier.create(broadcaster.resolveFromSchemaId(schemaId))
          .expectNext(new ErdMutationBroadcaster.ResolvedContext(
              projectId, schemaId))
          .verifyComplete();
    }

    @Test
    @DisplayName("schema가 없으면 empty를 반환한다")
    void empty_when_not_found() {
      given(getSchemaByIdPort.findSchemaById("unknown"))
          .willReturn(Mono.empty());

      StepVerifier.create(broadcaster.resolveFromSchemaId("unknown"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("resolveFromTableId")
  class ResolveFromTableId {

    @Test
    @DisplayName("정상적으로 context를 해소한다")
    void resolves() {
      String tableId = "table-1";
      String schemaId = "schema-1";
      String projectId = "project-1";

      given(getTableByIdPort.findTableById(tableId))
          .willReturn(Mono.just(new Table(tableId, schemaId,
              "test_table", "utf8mb4", "utf8mb4_general_ci")));
      given(getSchemaByIdPort.findSchemaById(schemaId))
          .willReturn(Mono.just(new Schema(schemaId, projectId,
              "mariadb", "test", "utf8mb4", "utf8mb4_general_ci")));

      StepVerifier.create(broadcaster.resolveFromTableId(tableId))
          .expectNext(new ErdMutationBroadcaster.ResolvedContext(
              projectId, schemaId))
          .verifyComplete();
    }

    @Test
    @DisplayName("table이 없으면 empty를 반환한다")
    void empty_when_table_not_found() {
      given(getTableByIdPort.findTableById("unknown"))
          .willReturn(Mono.empty());

      StepVerifier.create(broadcaster.resolveFromTableId("unknown"))
          .verifyComplete();
    }

  }

}
