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
import com.schemafy.domain.erd.column.domain.exception.ForeignKeyColumnProtectedException;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
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
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
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
  CreateConstraintUseCase createConstraintUseCase;

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

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "cascade_" + uniqueSuffix;

    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block().result();
    schemaId = schemaResult.id();

    var createPkTableCommand = new CreateTableCommand(
        schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci");
    var pkTableResult = createTableUseCase.createTable(createPkTableCommand).block().result();
    pkTableId = pkTableResult.tableId();

    var createFkTableCommand = new CreateTableCommand(
        schemaId, "fk_table", "utf8mb4", "utf8mb4_general_ci");
    var fkTableResult = createTableUseCase.createTable(createFkTableCommand).block().result();
    fkTableId = fkTableResult.tableId();

    var createPkColumnCommand = new CreateColumnCommand(
        pkTableId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var pkColumnResult = createColumnUseCase.createColumn(createPkColumnCommand).block().result();
    pkColumnId = pkColumnResult.columnId();

    var createPkColumn2Command = new CreateColumnCommand(
        pkTableId, "code", "VARCHAR", 50, null, null, 1, false, null, null, "PK2");
    var pkColumn2Result = createColumnUseCase.createColumn(createPkColumn2Command).block().result();
    pkColumn2Id = pkColumn2Result.columnId();

    var createPkConstraintCommand = new CreateConstraintCommand(
        pkTableId,
        "pk_pk_table",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(
            new CreateConstraintColumnCommand(pkColumnId, 0),
            new CreateConstraintColumnCommand(pkColumn2Id, 1)));
    createConstraintUseCase.createConstraint(createPkConstraintCommand).block();
  }

  @Nested
  @DisplayName("테이블 삭제 시")
  class DeleteTable {

    @Test
    @DisplayName("FK 테이블 삭제 시 관계와 관계 컬럼이 모두 삭제된다")
    void deletesRelationshipWhenFkTableDeleted() {
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(fkTableId)))
          .expectNextCount(1)
          .verifyComplete();

      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(relationship.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("PK 테이블 삭제 시 관계와 관계 컬럼이 모두 삭제된다")
    void deletesRelationshipWhenPkTableDeleted() {
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(pkTableId)))
          .expectNextCount(1)
          .verifyComplete();

      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(relationship.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("여러 관계가 있을 때 관련된 관계만 삭제된다")
    void deletesOnlyRelatedRelationships() {
      // FK 테이블 -> PK 테이블 관계
      createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      // FK 테이블 삭제
      StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(fkTableId)))
          .expectNextCount(1)
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
    @DisplayName("FK 컬럼은 직접 삭제할 수 없다")
    void rejectsDeletionOfForeignKeyColumn() {
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);
      String fkColumnToDelete = relationship.columns().stream()
          .filter(column -> column.pkColumnId().equals(pkColumnId))
          .findFirst()
          .orElseThrow()
          .fkColumnId();

      // FK 컬럼 직접 삭제 시 예외 발생
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(fkColumnToDelete)))
          .expectError(ForeignKeyColumnProtectedException.class)
          .verify();
    }

    @Test
    @DisplayName("PK 컬럼 삭제 시 해당 관계 컬럼이 삭제된다")
    void deletesRelationshipColumnWhenPkColumnDeleted() {
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      // PK 컬럼 삭제
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(pkColumnId)))
          .expectNextCount(1)
          .verifyComplete();

      // 관계는 남아있지만 컬럼은 하나만 남아야 함
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationship.relationshipId())))
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
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      // 관계 컬럼 ID 조회
      var columns = getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationship.relationshipId()))
          .block();
      assertThat(columns).hasSize(2);
      String firstColumnId = columns.get(0).id();
      String secondColumnId = columns.get(1).id();

      // 관계 컬럼 제거 (첫 번째)
      StepVerifier.create(removeRelationshipColumnUseCase.removeRelationshipColumn(
          new RemoveRelationshipColumnCommand(firstColumnId)))
          .expectNextCount(1)
          .verifyComplete();

      // 마지막 컬럼 제거
      StepVerifier.create(removeRelationshipColumnUseCase.removeRelationshipColumn(
          new RemoveRelationshipColumnCommand(secondColumnId)))
          .expectNextCount(1)
          .verifyComplete();

      // 관계도 삭제되어야 함
      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(relationship.relationshipId())))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("마지막이 아닌 컬럼을 제거하면 관계는 유지되고 위치가 재정렬된다")
    void repositionsColumnsWhenNonLastColumnRemoved() {
      var relationship = createAutoRelationship(RelationshipKind.NON_IDENTIFYING);

      // 첫 번째 관계 컬럼 ID 조회
      var columns = getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationship.relationshipId()))
          .block();
      assertThat(columns).hasSize(2);
      columns.sort((left, right) -> Integer.compare(left.seqNo(), right.seqNo()));
      String firstColumnId = columns.get(0).id();
      String remainingPkColumnId = columns.get(1).pkColumnId();

      // 첫 번째 컬럼 제거
      StepVerifier.create(removeRelationshipColumnUseCase.removeRelationshipColumn(
          new RemoveRelationshipColumnCommand(firstColumnId)))
          .expectNextCount(1)
          .verifyComplete();

      // 관계는 유지되고 남은 컬럼의 위치가 0으로 재정렬됨
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationship.relationshipId())))
          .assertNext(remainingColumns -> {
            assertThat(remainingColumns).hasSize(1);
            assertThat(remainingColumns.get(0).seqNo()).isEqualTo(0);
            assertThat(remainingColumns.get(0).pkColumnId()).isEqualTo(remainingPkColumnId);
          })
          .verifyComplete();
    }

  }

  private AutoRelationship createAutoRelationship(RelationshipKind kind) {
    var createCommand = new CreateRelationshipCommand(fkTableId,
        pkTableId,
        kind,
        Cardinality.ONE_TO_MANY);
    var result = createRelationshipUseCase.createRelationship(createCommand).block().result();
    var columns = getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(
            new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId()))
        .block();
    return new AutoRelationship(result.relationshipId(), columns);
  }

  private record AutoRelationship(String relationshipId, List<RelationshipColumn> columns) {
  }

}
