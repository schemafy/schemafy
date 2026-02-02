package com.schemafy.domain.erd.index.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Index Cascade 삭제 통합 테스트")
class IndexCascadeDeleteIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  DeleteTableUseCase deleteTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  DeleteColumnUseCase deleteColumnUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  GetIndexUseCase getIndexUseCase;

  @Autowired
  GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @Autowired
  GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  @Autowired
  RemoveIndexColumnUseCase removeIndexColumnUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;
  private String tableId;
  private String columnId1;
  private String columnId2;
  private String columnId3;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "index_cascade_" + uniqueSuffix;

    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
    schemaId = schemaResult.id();

    var createTableCommand = new CreateTableCommand(
        schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
    var tableResult = createTableUseCase.createTable(createTableCommand).block();
    tableId = tableResult.tableId();

    var createColumn1Command = new CreateColumnCommand(
        tableId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var column1Result = createColumnUseCase.createColumn(createColumn1Command).block();
    columnId1 = column1Result.columnId();

    var createColumn2Command = new CreateColumnCommand(
        tableId, "name", "VARCHAR", 100, null, null, 1, false, null, null, "Name");
    var column2Result = createColumnUseCase.createColumn(createColumn2Command).block();
    columnId2 = column2Result.columnId();

    var createColumn3Command = new CreateColumnCommand(
        tableId, "email", "VARCHAR", 255, null, null, 2, false, null, null, "Email");
    var column3Result = createColumnUseCase.createColumn(createColumn3Command).block();
    columnId3 = column3Result.columnId();
  }

  @Nested
  @DisplayName("테이블 삭제 시")
  class DeleteTable {

    @Test
    @DisplayName("테이블 삭제 시 관련 인덱스와 인덱스 컬럼이 모두 삭제된다")
    void deletesIndexesAndColumnsWhenTableDeleted() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(tableId)))
          .verifyComplete();

      StepVerifier.create(getIndexUseCase.getIndex(
          new GetIndexQuery(result.indexId())))
          .expectError(IndexNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("여러 인덱스가 있을 때 관련된 인덱스만 삭제된다")
    void deletesOnlyRelatedIndexes() {
      // 첫 번째 인덱스 생성
      var createCommand1 = new CreateIndexCommand(
          tableId, "idx_id", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      createIndexUseCase.createIndex(createCommand1).block();

      // 두 번째 인덱스 생성
      var createCommand2 = new CreateIndexCommand(
          tableId, "idx_name", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId2, 0, SortDirection.ASC)));
      createIndexUseCase.createIndex(createCommand2).block();

      // 테이블 삭제
      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(tableId)))
          .verifyComplete();

      // 모든 인덱스가 삭제되어야 함
      StepVerifier.create(getIndexesByTableIdUseCase.getIndexesByTableId(
          new GetIndexesByTableIdQuery(tableId)))
          .assertNext(indexes -> assertThat(indexes).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("컬럼 삭제 시")
  class DeleteColumn {

    @Test
    @DisplayName("컬럼 삭제 시 해당 인덱스 컬럼이 삭제된다")
    void deletesIndexColumnWhenColumnDeleted() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_composite", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.DESC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      // 컬럼 삭제
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(columnId1)))
          .verifyComplete();

      // 인덱스는 남아있지만 컬럼은 하나만 남아야 함
      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).columnId()).isEqualTo(columnId2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 인덱스에서 사용되는 컬럼 삭제 시 모든 관련 인덱스 컬럼이 삭제된다")
    void deletesAllIndexColumnsWhenColumnDeletedFromMultipleIndexes() {
      // 첫 번째 인덱스 (columnId1 사용)
      var createCommand1 = new CreateIndexCommand(
          tableId, "idx_id", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result1 = createIndexUseCase.createIndex(createCommand1).block();

      // 두 번째 인덱스 (columnId1, columnId2 사용)
      var createCommand2 = new CreateIndexCommand(
          tableId, "idx_composite", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.DESC)));
      var result2 = createIndexUseCase.createIndex(createCommand2).block();

      // columnId1 삭제
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(columnId1)))
          .verifyComplete();

      // 첫 번째 인덱스의 컬럼은 모두 삭제됨
      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result1.indexId())))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();

      // 두 번째 인덱스에는 columnId2만 남음
      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result2.indexId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).columnId()).isEqualTo(columnId2);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("마지막 인덱스 컬럼 제거 시")
  class RemoveLastIndexColumn {

    @Test
    @DisplayName("마지막 인덱스 컬럼을 제거하면 인덱스 자체가 삭제된다")
    void deletesIndexWhenLastColumnRemoved() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      // 인덱스 컬럼 ID 조회
      var columns = getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId()))
          .block();
      assertThat(columns).hasSize(1);
      String indexColumnId = columns.get(0).id();

      // 마지막 컬럼 제거
      StepVerifier.create(removeIndexColumnUseCase.removeIndexColumn(
          new RemoveIndexColumnCommand(result.indexId(), indexColumnId)))
          .verifyComplete();

      // 인덱스도 삭제되어야 함
      StepVerifier.create(getIndexUseCase.getIndex(
          new GetIndexQuery(result.indexId())))
          .expectError(IndexNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("마지막이 아닌 컬럼을 제거하면 인덱스는 유지되고 위치가 재정렬된다")
    void repositionsColumnsWhenNonLastColumnRemoved() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_composite", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.DESC),
              new CreateIndexColumnCommand(columnId3, 2, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      // 첫 번째 인덱스 컬럼 ID 조회
      var columns = getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId()))
          .block();
      assertThat(columns).hasSize(3);
      String firstColumnId = columns.get(0).id();

      // 첫 번째 컬럼 제거
      StepVerifier.create(removeIndexColumnUseCase.removeIndexColumn(
          new RemoveIndexColumnCommand(result.indexId(), firstColumnId)))
          .verifyComplete();

      // 인덱스는 유지되고 남은 컬럼의 위치가 재정렬됨
      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(remainingColumns -> {
            assertThat(remainingColumns).hasSize(2);
            assertThat(remainingColumns.get(0).seqNo()).isEqualTo(0);
            assertThat(remainingColumns.get(1).seqNo()).isEqualTo(1);
          })
          .verifyComplete();
    }

  }

}
