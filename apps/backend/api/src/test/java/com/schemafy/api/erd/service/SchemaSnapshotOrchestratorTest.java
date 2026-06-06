package com.schemafy.api.erd.service;

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
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionResult;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaSnapshotOrchestrator")
class SchemaSnapshotOrchestratorTest {

  @Mock
  GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;

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
        getTablesBySchemaIdUseCase,
        tableSnapshotOrchestrator,
        transactionManager);
  }

  @Test
  @DisplayName("schema revision과 strict table snapshots를 함께 반환한다")
  void returnsRevisionAndSnapshots() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "mariadb", "main_schema",
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

    StepVerifier.create(sut.getSchemaSnapshots(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(42L);
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
    Schema schema = new Schema(schemaId, "project-1", "mariadb", "main_schema",
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
    Schema schema = new Schema(schemaId, "project-1", "mariadb", "main_schema",
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

}
