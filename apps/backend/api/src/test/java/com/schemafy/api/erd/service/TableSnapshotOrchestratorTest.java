package com.schemafy.api.erd.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.api.erd.service.relationship.RelationshipApiResponseMapper;
import com.schemafy.api.erd.service.table.TableApiResponseMapper;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.core.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.core.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.core.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.table.application.port.in.GetTableQuery;
import com.schemafy.core.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TableSnapshotOrchestrator")
class TableSnapshotOrchestratorTest {

  @Mock
  GetTableUseCase getTableUseCase;

  @Mock
  GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @Mock
  GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  @Mock
  GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;

  @Mock
  GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  @Mock
  GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @Mock
  GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @Mock
  GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  TableSnapshotOrchestrator sut;

  @BeforeEach
  void setUp() {
    JsonCodec jsonCodec = new JsonCodec(new ObjectMapper()
        .findAndRegisterModules());
    sut = new TableSnapshotOrchestrator(
        getTableUseCase,
        getColumnsByTableIdUseCase,
        getConstraintsByTableIdUseCase,
        getConstraintColumnsByConstraintIdUseCase,
        getRelationshipsByTableIdUseCase,
        getRelationshipColumnsByRelationshipIdUseCase,
        getIndexesByTableIdUseCase,
        getIndexColumnsByIndexIdUseCase,
        new TableApiResponseMapper(jsonCodec),
        new RelationshipApiResponseMapper(jsonCodec));
  }

  @Test
  @DisplayName("getTableSnapshot: 조합 결과를 구성하고 seqNo 기준으로 정렬한다")
  void getTableSnapshot_composesAndSorts() {
    String tableId = "table-1";
    Table table = new Table(tableId, "schema-1", "users", "utf8mb4",
        "utf8mb4_general_ci", "{\"x\":10}");

    Column column2 = new Column("c2", tableId, "name", "VARCHAR",
        new ColumnTypeArguments(30, null, null), 2, false, null, null, null);
    Column column1 = new Column("c1", tableId, "id", "BIGINT",
        new ColumnTypeArguments(20, null, null), 1, true, null, null, "pk");

    Constraint constraint = new Constraint("ct1", tableId, "pk_users",
        ConstraintKind.PRIMARY_KEY, null, null);
    ConstraintColumn cc2 = new ConstraintColumn("cc2", "ct1", "c2", 2);
    ConstraintColumn cc1 = new ConstraintColumn("cc1", "ct1", "c1", 1);

    Relationship relationship = new Relationship("r1", "pk-table-1",
        tableId, "fk_users_orders", RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY, null);
    RelationshipColumn rc2 = new RelationshipColumn("rc2", "r1", "pk-c2",
        "c2", 2);
    RelationshipColumn rc1 = new RelationshipColumn("rc1", "r1", "pk-c1",
        "c1", 1);

    Index index = new Index("i1", tableId, "idx_users_id", IndexType.BTREE);
    IndexColumn ic2 = new IndexColumn("ic2", "i1", "c2", 2, SortDirection.DESC);
    IndexColumn ic1 = new IndexColumn("ic1", "i1", "c1", 1, SortDirection.ASC);

    when(getTableUseCase.getTable(any(GetTableQuery.class)))
        .thenReturn(Mono.just(table));
    when(getColumnsByTableIdUseCase.getColumnsByTableId(any()))
        .thenReturn(Mono.just(List.of(column2, column1)));
    when(getConstraintsByTableIdUseCase.getConstraintsByTableId(any()))
        .thenReturn(Mono.just(List.of(constraint)));
    when(getConstraintColumnsByConstraintIdUseCase
        .getConstraintColumnsByConstraintId(any()))
        .thenReturn(Mono.just(List.of(cc2, cc1)));
    when(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(any()))
        .thenReturn(Mono.just(List.of(relationship)));
    when(getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(any()))
        .thenReturn(Mono.just(List.of(rc2, rc1)));
    when(getIndexesByTableIdUseCase.getIndexesByTableId(any()))
        .thenReturn(Mono.just(List.of(index)));
    when(getIndexColumnsByIndexIdUseCase.getIndexColumnsByIndexId(any()))
        .thenReturn(Mono.just(List.of(ic2, ic1)));

    StepVerifier.create(sut.getTableSnapshot(tableId))
        .assertNext(snapshot -> {
          assertThat(snapshot.table().id()).isEqualTo(tableId);
          assertThat(snapshot.columns()).extracting("id")
              .containsExactly("c1", "c2");
          assertThat(snapshot.constraints()).hasSize(1);
          assertThat(snapshot.constraints().get(0).columns()).extracting("id")
              .containsExactly("cc1", "cc2");
          assertThat(snapshot.relationships()).hasSize(1);
          assertThat(snapshot.relationships().get(0).columns()).extracting("id")
              .containsExactly("rc1", "rc2");
          assertThat(snapshot.indexes()).hasSize(1);
          assertThat(snapshot.indexes().get(0).columns()).extracting("id")
              .containsExactly("ic1", "ic2");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTableSnapshot: 하위 결과가 비어있으면 빈 리스트를 유지한다")
  void getTableSnapshot_emptyChildren() {
    String tableId = "table-1";
    Table table = new Table(tableId, "schema-1", "users", "utf8mb4",
        "utf8mb4_general_ci");

    when(getTableUseCase.getTable(any(GetTableQuery.class)))
        .thenReturn(Mono.just(table));
    when(getColumnsByTableIdUseCase.getColumnsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getConstraintsByTableIdUseCase.getConstraintsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getIndexesByTableIdUseCase.getIndexesByTableId(any()))
        .thenReturn(Mono.just(List.of()));

    StepVerifier.create(sut.getTableSnapshot(tableId))
        .assertNext(snapshot -> {
          assertThat(snapshot.table().id()).isEqualTo(tableId);
          assertThat(snapshot.columns()).isEmpty();
          assertThat(snapshot.constraints()).isEmpty();
          assertThat(snapshot.relationships()).isEmpty();
          assertThat(snapshot.indexes()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTableSnapshots: 일부 테이블이 실패하면 성공한 결과만 포함한다")
  void getTableSnapshots_partialSuccess() {
    String tableId1 = "table-1";
    String tableId2 = "table-2";

    when(getTableUseCase.getTable(any(GetTableQuery.class)))
        .thenAnswer(invocation -> {
          GetTableQuery query = invocation.getArgument(0);
          if (tableId1.equals(query.tableId())) {
            return Mono.just(new Table(tableId1, "schema-1", "users",
                "utf8mb4", "utf8mb4_general_ci"));
          }
          return Mono.error(new IllegalStateException("boom"));
        });
    when(getColumnsByTableIdUseCase.getColumnsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getConstraintsByTableIdUseCase.getConstraintsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getIndexesByTableIdUseCase.getIndexesByTableId(any()))
        .thenReturn(Mono.just(List.of()));

    StepVerifier.create(sut.getTableSnapshots(List.of(tableId1, tableId2)))
        .assertNext(snapshots -> {
          assertThat(snapshots).hasSize(1);
          assertThat(snapshots).containsKey(tableId1);
          assertThat(snapshots).doesNotContainKey(tableId2);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("getTableSnapshotsStrict: 일부 테이블이 실패하면 전체를 실패시킨다")
  void getTableSnapshotsStrict_failsOnPartialFailure() {
    String tableId1 = "table-1";
    String tableId2 = "table-2";

    when(getTableUseCase.getTable(any(GetTableQuery.class)))
        .thenAnswer(invocation -> {
          GetTableQuery query = invocation.getArgument(0);
          if (tableId1.equals(query.tableId())) {
            return Mono.just(new Table(tableId1, "schema-1", "users",
                "utf8mb4", "utf8mb4_general_ci"));
          }
          return Mono.error(new IllegalStateException("boom"));
        });
    when(getColumnsByTableIdUseCase.getColumnsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getConstraintsByTableIdUseCase.getConstraintsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(any()))
        .thenReturn(Mono.just(List.of()));
    when(getIndexesByTableIdUseCase.getIndexesByTableId(any()))
        .thenReturn(Mono.just(List.of()));

    StepVerifier.create(sut.getTableSnapshotsStrict(List.of(tableId1, tableId2)))
        .expectErrorMessage("boom")
        .verify();
  }

}
