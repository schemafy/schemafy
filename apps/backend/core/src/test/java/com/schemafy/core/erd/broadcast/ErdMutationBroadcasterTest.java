package com.schemafy.core.erd.broadcast;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.collaboration.dto.event.CollaborationOutbound;
import com.schemafy.core.collaboration.service.CollaborationEventPublisher;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdMutationBroadcaster 단위 테스트")
class ErdMutationBroadcasterTest {

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
    @DisplayName("정상적으로 이벤트를 발행한다")
    void publishes_event() {
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

      StepVerifier.create(broadcaster.broadcast(tableIds))
          .verifyComplete();

      verify(eventPublisher).publish(eq(projectId),
          any(CollaborationOutbound.class));
    }

    @Test
    @DisplayName("빈 affectedTableIds이면 skip한다")
    void skips_when_empty_table_ids() {
      StepVerifier.create(broadcaster.broadcast(Set.of()))
          .verifyComplete();

      verify(getTableByIdPort, never()).findTableById(any());
    }

    @Test
    @DisplayName("null이면 skip한다")
    void skips_when_null() {
      StepVerifier.create(broadcaster.broadcast(null))
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

      StepVerifier.create(broadcaster.broadcast(tableIds))
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

      StepVerifier.create(broadcaster.broadcast(tableIds))
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

      StepVerifier.create(broadcaster.broadcastSchemaChange(schemaId))
          .verifyComplete();

      verify(eventPublisher).publish(eq(projectId),
          any(CollaborationOutbound.class));
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
          broadcaster.broadcastWithContext(ctx, tableIds))
          .verifyComplete();

      verify(eventPublisher).publish(eq(projectId),
          any(CollaborationOutbound.class));
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
