package com.schemafy.domain.erd.relationship.integration;

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
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
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
@DisplayName("Relationship Cascade 삭제 통합 테스트")
class RelationshipCascadeDeleteIntegrationTest {

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
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  GetRelationshipUseCase getRelationshipUseCase;

  @Autowired
  GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  @Autowired
  GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @Autowired
  RemoveRelationshipColumnUseCase removeRelationshipColumnUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;
  private String pkTableId;
  private String fkTableId;
  private String pkColumnId;
  private String pkColumn2Id;
  private String fkColumnId;
  private String fkColumn2Id;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "cascade_" + uniqueSuffix;

    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
    schemaId = schemaResult.id();

    var createPkTableCommand = new CreateTableCommand(
        schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci");
    var pkTableResult = createTableUseCase.createTable(createPkTableCommand).block();
    pkTableId = pkTableResult.tableId();

    var createFkTableCommand = new CreateTableCommand(
        schemaId, "fk_table", "utf8mb4", "utf8mb4_general_ci");
    var fkTableResult = createTableUseCase.createTable(createFkTableCommand).block();
    fkTableId = fkTableResult.tableId();

    var createPkColumnCommand = new CreateColumnCommand(
        pkTableId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var pkColumnResult = createColumnUseCase.createColumn(createPkColumnCommand).block();
    pkColumnId = pkColumnResult.columnId();

    var createPkColumn2Command = new CreateColumnCommand(
        pkTableId, "code", "VARCHAR", 50, null, null, 1, false, null, null, "PK2");
    var pkColumn2Result = createColumnUseCase.createColumn(createPkColumn2Command).block();
    pkColumn2Id = pkColumn2Result.columnId();

    var createFkColumnCommand = new CreateColumnCommand(
        fkTableId, "pk_id", "INT", null, null, null, 0, false, null, null, "FK");
    var fkColumnResult = createColumnUseCase.createColumn(createFkColumnCommand).block();
    fkColumnId = fkColumnResult.columnId();

    var createFkColumn2Command = new CreateColumnCommand(
        fkTableId, "pk_code", "VARCHAR", 50, null, null, 1, false, null, null, "FK2");
    var fkColumn2Result = createColumnUseCase.createColumn(createFkColumn2Command).block();
    fkColumn2Id = fkColumn2Result.columnId();
  }

  @Nested
  @DisplayName("테이블 삭제 시")
  class DeleteTable {

    @Test
    @DisplayName("FK 테이블 삭제 시 관계와 관계 컬럼이 모두 삭제된다")
    void deletesRelationshipWhenFkTableDeleted() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(fkTableId)))
          .verifyComplete();

      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(result.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("PK 테이블 삭제 시 관계와 관계 컬럼이 모두 삭제된다")
    void deletesRelationshipWhenPkTableDeleted() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(pkTableId)))
          .verifyComplete();

      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(result.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("여러 관계가 있을 때 관련된 관계만 삭제된다")
    void deletesOnlyRelatedRelationships() {
      // FK 테이블 -> PK 테이블 관계
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)));
      createRelationshipUseCase.createRelationship(createCommand).block();

      // FK 테이블 삭제
      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(fkTableId)))
          .verifyComplete();

      // FK 테이블 관련 관계는 없어야 함
      StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
          new GetRelationshipsByTableIdQuery(fkTableId)))
          .assertNext(relationships -> assertThat(relationships).isEmpty())
          .verifyComplete();

      // PK 테이블은 여전히 존재
      StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
          new GetRelationshipsByTableIdQuery(pkTableId)))
          .assertNext(relationships -> assertThat(relationships).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("컬럼 삭제 시")
  class DeleteColumn {

    @Test
    @DisplayName("FK 컬럼 삭제 시 해당 관계 컬럼이 삭제된다")
    void deletesRelationshipColumnWhenFkColumnDeleted() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0),
              new CreateRelationshipColumnCommand(pkColumn2Id, fkColumn2Id, 1)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      // FK 컬럼 삭제
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(fkColumnId)))
          .verifyComplete();

      // 관계는 남아있지만 컬럼은 하나만 남아야 함
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).fkColumnId()).isEqualTo(fkColumn2Id);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("PK 컬럼 삭제 시 해당 관계 컬럼이 삭제된다")
    void deletesRelationshipColumnWhenPkColumnDeleted() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0),
              new CreateRelationshipColumnCommand(pkColumn2Id, fkColumn2Id, 1)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      // PK 컬럼 삭제
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(pkColumnId)))
          .verifyComplete();

      // 관계는 남아있지만 컬럼은 하나만 남아야 함
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumn2Id);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("마지막 관계 컬럼 제거 시")
  class RemoveLastRelationshipColumn {

    @Test
    @DisplayName("마지막 관계 컬럼을 제거하면 관계 자체가 삭제된다")
    void deletesRelationshipWhenLastColumnRemoved() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      // 관계 컬럼 ID 조회
      var columns = getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId()))
          .block();
      assertThat(columns).hasSize(1);
      String columnId = columns.get(0).id();

      // 마지막 컬럼 제거
      StepVerifier.create(removeRelationshipColumnUseCase.removeRelationshipColumn(
          new RemoveRelationshipColumnCommand(result.relationshipId(), columnId)))
          .verifyComplete();

      // 관계도 삭제되어야 함
      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(result.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("마지막이 아닌 컬럼을 제거하면 관계는 유지되고 위치가 재정렬된다")
    void repositionsColumnsWhenNonLastColumnRemoved() {
      var createCommand = new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_test",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0),
              new CreateRelationshipColumnCommand(pkColumn2Id, fkColumn2Id, 1)));
      var result = createRelationshipUseCase.createRelationship(createCommand).block();

      // 첫 번째 관계 컬럼 ID 조회
      var columns = getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId()))
          .block();
      assertThat(columns).hasSize(2);
      String firstColumnId = columns.get(0).id();

      // 첫 번째 컬럼 제거
      StepVerifier.create(removeRelationshipColumnUseCase.removeRelationshipColumn(
          new RemoveRelationshipColumnCommand(result.relationshipId(), firstColumnId)))
          .verifyComplete();

      // 관계는 유지되고 남은 컬럼의 위치가 0으로 재정렬됨
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId())))
          .assertNext(remainingColumns -> {
            assertThat(remainingColumns).hasSize(1);
            assertThat(remainingColumns.get(0).seqNo()).isEqualTo(0);
            assertThat(remainingColumns.get(0).fkColumnId()).isEqualTo(fkColumn2Id);
          })
          .verifyComplete();
    }

  }

}
