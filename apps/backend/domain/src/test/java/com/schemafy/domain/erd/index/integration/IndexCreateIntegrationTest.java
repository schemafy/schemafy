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
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.AddIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.DeleteIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexColumnsByIndexIdUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Index 생성 및 관리 통합 테스트")
class IndexCreateIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  GetIndexUseCase getIndexUseCase;

  @Autowired
  GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @Autowired
  GetIndexColumnsByIndexIdUseCase getIndexColumnsByIndexIdUseCase;

  @Autowired
  ChangeIndexNameUseCase changeIndexNameUseCase;

  @Autowired
  ChangeIndexTypeUseCase changeIndexTypeUseCase;

  @Autowired
  AddIndexColumnUseCase addIndexColumnUseCase;

  @Autowired
  ChangeIndexColumnPositionUseCase changeIndexColumnPositionUseCase;

  @Autowired
  ChangeIndexColumnSortDirectionUseCase changeIndexColumnSortDirectionUseCase;

  @Autowired
  DeleteIndexUseCase deleteIndexUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;
  private String tableId;
  private String columnId1;
  private String columnId2;
  private String columnId3;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "index_create_" + uniqueSuffix;

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
  @DisplayName("인덱스 생성 시")
  class CreateIndex {

    @Test
    @DisplayName("BTREE 인덱스를 생성한다")
    void createsBtreeIndex() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_btree", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));

      StepVerifier.create(createIndexUseCase.createIndex(createCommand))
          .assertNext(result -> {
            assertThat(result.indexId()).isNotNull();
            assertThat(result.name()).isEqualTo("idx_btree");
            assertThat(result.type()).isEqualTo(IndexType.BTREE);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("HASH 인덱스를 생성한다")
    void createsHashIndex() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_hash", IndexType.HASH,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));

      StepVerifier.create(createIndexUseCase.createIndex(createCommand))
          .assertNext(result -> {
            assertThat(result.indexId()).isNotNull();
            assertThat(result.type()).isEqualTo(IndexType.HASH);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("FULLTEXT 인덱스를 생성한다")
    void createsFulltextIndex() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_fulltext", IndexType.FULLTEXT,
          List.of(new CreateIndexColumnCommand(columnId2, 0, SortDirection.ASC)));

      StepVerifier.create(createIndexUseCase.createIndex(createCommand))
          .assertNext(result -> {
            assertThat(result.indexId()).isNotNull();
            assertThat(result.type()).isEqualTo(IndexType.FULLTEXT);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("복합 컬럼 인덱스를 생성한다")
    void createsCompositeIndex() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_composite", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.DESC)));

      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns.get(0).columnId()).isEqualTo(columnId1);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(0).sortDirection()).isEqualTo(SortDirection.ASC);
            assertThat(columns.get(1).columnId()).isEqualTo(columnId2);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
            assertThat(columns.get(1).sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 이름 변경 시")
  class ChangeIndexName {

    @Test
    @DisplayName("인덱스 이름을 변경한다")
    void changesIndexName() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_old", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(changeIndexNameUseCase.changeIndexName(
          new ChangeIndexNameCommand(result.indexId(), "idx_new")))
          .verifyComplete();

      StepVerifier.create(getIndexUseCase.getIndex(
          new GetIndexQuery(result.indexId())))
          .assertNext(index -> assertThat(index.name()).isEqualTo("idx_new"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 타입 변경 시")
  class ChangeIndexType {

    @Test
    @DisplayName("인덱스 타입을 변경한다")
    void changesIndexType() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(changeIndexTypeUseCase.changeIndexType(
          new ChangeIndexTypeCommand(result.indexId(), IndexType.HASH)))
          .verifyComplete();

      StepVerifier.create(getIndexUseCase.getIndex(
          new GetIndexQuery(result.indexId())))
          .assertNext(index -> assertThat(index.type()).isEqualTo(IndexType.HASH))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 컬럼 추가 시")
  class AddIndexColumn {

    @Test
    @DisplayName("기존 인덱스에 컬럼을 추가한다")
    void addsColumnToIndex() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(addIndexColumnUseCase.addIndexColumn(
          new AddIndexColumnCommand(result.indexId(), columnId2, 1, SortDirection.DESC)))
          .assertNext(addResult -> {
            assertThat(addResult.indexColumnId()).isNotNull();
            assertThat(addResult.columnId()).isEqualTo(columnId2);
            assertThat(addResult.seqNo()).isEqualTo(1);
            assertThat(addResult.sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();

      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(columns -> assertThat(columns).hasSize(2))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 컬럼 위치 변경 시")
  class ChangeIndexColumnPosition {

    @Test
    @DisplayName("인덱스 컬럼의 위치를 변경한다")
    void changesColumnPosition() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_composite", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId3, 2, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      // 현재 컬럼 조회
      var columns = getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId()))
          .block();
      assertThat(columns).hasSize(3);

      // 마지막 컬럼(seqNo 2)을 첫 번째(seqNo 0)로 이동
      String lastColumnId = columns.get(2).id();

      StepVerifier.create(changeIndexColumnPositionUseCase.changeIndexColumnPosition(
          new ChangeIndexColumnPositionCommand(lastColumnId, 0)))
          .verifyComplete();

      // 위치 확인
      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(reorderedColumns -> {
            assertThat(reorderedColumns).hasSize(3);
            assertThat(reorderedColumns.get(0).columnId()).isEqualTo(columnId3);
            assertThat(reorderedColumns.get(0).seqNo()).isEqualTo(0);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 컬럼 정렬 방향 변경 시")
  class ChangeIndexColumnSortDirection {

    @Test
    @DisplayName("인덱스 컬럼의 정렬 방향을 변경한다")
    void changesSortDirection() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      var columns = getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId()))
          .block();
      String indexColumnId = columns.get(0).id();

      StepVerifier.create(changeIndexColumnSortDirectionUseCase.changeIndexColumnSortDirection(
          new ChangeIndexColumnSortDirectionCommand(indexColumnId, SortDirection.DESC)))
          .verifyComplete();

      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(updatedColumns -> {
            assertThat(updatedColumns.get(0).sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("인덱스 삭제 시")
  class DeleteIndex {

    @Test
    @DisplayName("인덱스와 관련 컬럼을 모두 삭제한다")
    void deletesIndexAndColumns() {
      var createCommand = new CreateIndexCommand(
          tableId, "idx_test", IndexType.BTREE,
          List.of(
              new CreateIndexColumnCommand(columnId1, 0, SortDirection.ASC),
              new CreateIndexColumnCommand(columnId2, 1, SortDirection.DESC)));
      var result = createIndexUseCase.createIndex(createCommand).block();

      StepVerifier.create(deleteIndexUseCase.deleteIndex(
          new DeleteIndexCommand(result.indexId())))
          .verifyComplete();

      StepVerifier.create(getIndexUseCase.getIndex(
          new GetIndexQuery(result.indexId())))
          .expectError(IndexNotExistException.class)
          .verify();

      StepVerifier.create(getIndexColumnsByIndexIdUseCase
          .getIndexColumnsByIndexId(
              new GetIndexColumnsByIndexIdQuery(result.indexId())))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

  }

}
