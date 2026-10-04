package com.schemafy.api.erd.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import reactor.util.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaSnapshotReader")
class SchemaSnapshotReaderTest {

  @Mock
  GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;

  @Mock
  GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;

  @Mock
  TableSnapshotOrchestrator tableSnapshotOrchestrator;

  @Mock
  ReactiveTransactionManager transactionManager;

  @Mock
  ReactiveTransaction transaction;

  SchemaSnapshotReader sut;

  @BeforeEach
  void setUp() {
    given(transactionManager.getReactiveTransaction(any()))
        .willReturn(Mono.just(transaction));
    lenient().when(transactionManager.commit(transaction))
        .thenReturn(Mono.empty());
    lenient().when(transactionManager.rollback(transaction))
        .thenReturn(Mono.empty());

    sut = new SchemaSnapshotReader(
        getSchemaWithRevisionUseCase,
        getTablesBySchemaIdUseCase,
        tableSnapshotOrchestrator,
        transactionManager);
  }

  @Test
  @DisplayName("schema 도메인, revision, strict table snapshots를 함께 반환한다")
  void readsSchemaRevisionAndStrictTableSnapshots() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table1 = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");
    Table table2 = new Table("table-2", schemaId, "orders", "utf8mb4",
        "utf8mb4_general_ci");
    TableSnapshotResponse snapshot1 = tableSnapshot(table1, schemaId);
    TableSnapshotResponse snapshot2 = tableSnapshot(table2, schemaId);

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 42L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table1, table2));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willReturn(Mono.just(Map.of(
            table1.id(), snapshot1,
            table2.id(), snapshot2)));

    StepVerifier.create(sut.readSchemaSnapshot(schemaId))
        .assertNext(result -> {
          assertThat(result.schema()).isSameAs(schema);
          assertThat(result.currentRevision()).isEqualTo(42L);
          assertThat(result.tableSnapshots())
              .containsEntry(table1.id(), snapshot1)
              .containsEntry(table2.id(), snapshot2);
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
  @DisplayName("테이블이 없으면 strict 조회 없이 빈 snapshots를 반환한다")
  void readsEmptySnapshotsWithoutStrictLookup() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "empty_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 7L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.empty());

    StepVerifier.create(sut.readSchemaSnapshot(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(7L);
          assertThat(result.tableSnapshots()).isEmpty();
        })
        .verifyComplete();

    then(tableSnapshotOrchestrator).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("strict snapshot 조회 실패를 그대로 전파하고 read transaction을 rollback한다")
  void propagatesStrictSnapshotFailureAndRollsBack() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    Table table = new Table("table-1", schemaId, "users", "utf8mb4",
        "utf8mb4_general_ci");
    RuntimeException failure = new RuntimeException("snapshot failed");

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.just(new GetSchemaWithRevisionResult(schema, 7L)));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.just(table));
    given(tableSnapshotOrchestrator.getTableSnapshotsStrict(anyList()))
        .willReturn(Mono.error(failure));

    StepVerifier.create(sut.readSchemaSnapshot(schemaId))
        .expectErrorMatches(error -> error == failure)
        .verify();

    then(transactionManager).should().rollback(transaction);
  }

  @Test
  @DisplayName("구독할 때마다 조회를 다시 수행하고 reactor context를 전파한다")
  void isLazyAndPropagatesReactorContext() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    AtomicInteger reads = new AtomicInteger();
    AtomicInteger markedReads = new AtomicInteger();

    given(getSchemaWithRevisionUseCase.getSchemaWithRevision(any(GetSchemaQuery.class)))
        .willReturn(Mono.deferContextual(contextView -> {
          reads.incrementAndGet();
          if (contextView.hasKey("marker")) {
            markedReads.incrementAndGet();
          }
          return Mono.just(new GetSchemaWithRevisionResult(schema, 3L));
        }));
    given(getTablesBySchemaIdUseCase.getTablesBySchemaId(any(GetTablesBySchemaIdQuery.class)))
        .willReturn(Flux.empty());

    Mono<SchemaSnapshotReadResult> read = sut.readSchemaSnapshot(schemaId)
        .contextWrite(Context.of("marker", true));

    StepVerifier.create(read).expectNextCount(1).verifyComplete();
    StepVerifier.create(read).expectNextCount(1).verifyComplete();

    assertThat(reads.get()).isEqualTo(2);
    assertThat(markedReads.get()).isEqualTo(2);
  }

  private static TableSnapshotResponse tableSnapshot(Table table,
      String schemaId) {
    return new TableSnapshotResponse(
        new TableResponse(table.id(), schemaId, table.name(), table.charset(),
            table.collation(), null),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

}
