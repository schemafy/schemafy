package com.schemafy.api.erd.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionResult;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaSnapshotOrchestrator")
class SchemaSnapshotOrchestratorTest {

  @Mock
  GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;

  @Mock
  GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;

  @Mock
  TableSnapshotOrchestrator tableSnapshotOrchestrator;

  @Mock
  ReactiveTransactionManager transactionManager;

  SchemaSnapshotOrchestrator sut;

  @Mock
  ReactiveTransaction transaction;

  @BeforeEach
  void setUpTransaction() {
    given(transactionManager.getReactiveTransaction(any()))
        .willReturn(Mono.just(transaction));
    lenient().when(transactionManager.commit(transaction))
        .thenReturn(Mono.empty());
    lenient().when(transactionManager.rollback(transaction))
        .thenReturn(Mono.empty());

    sut = new SchemaSnapshotOrchestrator(
        getSchemaWithRevisionUseCase,
        getSchemaByIdPort,
        findSchemaCollaborationStatePort,
        getTablesBySchemaIdUseCase,
        tableSnapshotOrchestrator,
        transactionManager);
  }

  @Test
  @DisplayName("schema metadata, revision과 strict table snapshots를 함께 반환한다")
  void returnsCompleteSchemaState() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table1 = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");
    Table table2 = new Table("table-2", schemaId, "orders", "utf8mb4",
        "utf8mb4_general_ci");
    TableSnapshotResponse snapshot1 = new TableSnapshotResponse(
        new TableResponse(table1.id(), schemaId, table1.name(), table1.charset(),
            table1.collation(), null),
        List.of(),
        List.of(),
        List.of(),
        List.of());
    TableSnapshotResponse snapshot2 = new TableSnapshotResponse(
        new TableResponse(table2.id(), schemaId, table2.name(), table2.charset(),
            table2.collation(), null),
        List.of(),
        List.of(),
        List.of(),
        List.of());

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 42L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table1, table2));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willAnswer(invocation -> {
          List<String> tableIds = invocation.getArgument(0);
          assertThat(tableIds).containsExactlyInAnyOrder(table1.id(), table2.id());
          return Mono.just(Map.of(
              table1.id(), snapshot1,
              table2.id(), snapshot2));
        });

    StepVerifier.create(sut.getSchemaState(schemaId))
        .assertNext(result -> {
          assertThat(result.schema().id()).isEqualTo(schemaId);
          assertThat(result.schema().projectId()).isEqualTo("project-1");
          assertThat(result.schema().name()).isEqualTo("main_schema");
          assertThat(result.schema().charset()).isEqualTo("utf8mb4");
          assertThat(result.schema().collation())
              .isEqualTo("utf8mb4_general_ci");
          assertThat(result.schema().currentRevision()).isNull();
          assertThat(result.revision()).isEqualTo(42L);
          assertThat(result.snapshots()).containsEntry(table1.id(), snapshot1);
          assertThat(result.snapshots()).containsEntry(table2.id(), snapshot2);
        })
        .verifyComplete();

    then(getSchemaWithRevisionUseCase).should()
        .getSchemaWithRevision(new GetSchemaQuery(schemaId));
    then(getTablesBySchemaIdUseCase).should()
        .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId));
    then(tableSnapshotOrchestrator).should()
        .getTableSnapshotsStrict(argThat(tableIds -> tableIds.size() == 2
            && tableIds.containsAll(List.of(table1.id(), table2.id()))));
  }

  @Test
  @DisplayName("테이블이 없으면 빈 snapshots를 반환한다")
  void returnsEmptySnapshotsWhenSchemaHasNoTables() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 7L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.empty());

    StepVerifier.create(sut.getSchemaSnapshots(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(7L);
          assertThat(result.snapshots()).isEmpty();
        })
        .verifyComplete();

    then(tableSnapshotOrchestrator).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("strict snapshot 조회 실패는 그대로 전파한다")
  void propagatesStrictSnapshotFailure() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 7L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willReturn(Mono.error(new IllegalStateException("snapshot failed")));

    StepVerifier.create(sut.getSchemaSnapshots(schemaId))
        .expectErrorMessage("snapshot failed")
        .verify();
  }

  @Test
  @DisplayName("getSchemaStateForSnapshotWorker는 access-gated use case를 거치지 않고 raw port로 직접 조회한다")
  void snapshotWorkerLookupBypassesAccessGatedUseCase() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");
    TableSnapshotResponse snapshot = new TableSnapshotResponse(
        new TableResponse(table.id(), schemaId, table.name(), table.charset(),
            table.collation(), null),
        List.of(), List.of(), List.of(), List.of());

    given(getSchemaByIdPort.findSchemaById(schemaId))
        .willReturn(Mono.just(schema));
    given(findSchemaCollaborationStatePort.findBySchemaId(schemaId))
        .willReturn(Mono.just(new SchemaCollaborationState(schemaId,
            "project-1", 42L, Instant.now(), Instant.now())));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willReturn(Mono.just(Map.of(table.id(), snapshot)));

    StepVerifier.create(sut.getSchemaStateForSnapshotWorker(schemaId))
        .assertNext(result -> {
          assertThat(result.schema().id()).isEqualTo(schemaId);
          assertThat(result.revision()).isEqualTo(42L);
          assertThat(result.snapshots()).containsEntry(table.id(), snapshot);
        })
        .verifyComplete();

    then(getSchemaWithRevisionUseCase).should(never())
        .getSchemaWithRevision(any(GetSchemaQuery.class));
  }

  @Test
  @DisplayName("getSchemaStateForSnapshotWorker는 collaboration state가 없으면 revision 0으로 취급한다")
  void snapshotWorkerLookupDefaultsToRevisionZeroWithoutCollaborationState() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(getSchemaByIdPort.findSchemaById(schemaId))
        .willReturn(Mono.just(schema));
    given(findSchemaCollaborationStatePort.findBySchemaId(schemaId))
        .willReturn(Mono.empty());
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.empty());

    StepVerifier.create(sut.getSchemaStateForSnapshotWorker(schemaId))
        .assertNext(result -> assertThat(result.revision()).isEqualTo(0L))
        .verifyComplete();
  }

  @Test
  @DisplayName("getSchemaStateForSnapshotWorker는 schema가 없으면 NOT_FOUND DomainException을 던진다")
  void snapshotWorkerLookupFailsWhenSchemaMissing() {
    String schemaId = "schema-missing";
    given(getSchemaByIdPort.findSchemaById(schemaId))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.getSchemaStateForSnapshotWorker(schemaId))
        .expectErrorMatches(error -> error instanceof DomainException)
        .verify();

    then(findSchemaCollaborationStatePort).should(never())
        .findBySchemaId(eq(schemaId));
  }

}
