package com.schemafy.domain.erd.constraint.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.PatchField;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/** PK Constraint 컬럼 변경이 Relationship에 미치는 영향 테스트 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Constraint-Relationship 연쇄 삭제 통합 테스트")
class ConstraintRelationshipIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;

  @Autowired
  RemoveConstraintColumnUseCase removeConstraintColumnUseCase;

  @Autowired
  AddConstraintColumnUseCase addConstraintColumnUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  GetRelationshipUseCase getRelationshipUseCase;

  @Autowired
  GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @Autowired
  GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @Autowired
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Autowired
  ChangeColumnMetaUseCase changeColumnMetaUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;

  // PK Table (참조되는 테이블)
  private String pkTableId;
  private String pkColumnId1;
  private String pkColumnId2;
  private String pkConstraintId;

  // FK Table 1 (참조하는 테이블)
  private String fkTableId1;

  // FK Table 2 (참조하는 테이블 - 다중 Relationship 테스트용)
  private String fkTableId2;

  @BeforeEach
  void setUp() {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "constraint_rel_" + uniqueSuffix;

    // Schema 생성
    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block().result();
    schemaId = schemaResult.id();

    // PK Table 생성 (복합키를 가질 테이블)
    var createPkTableCommand = new CreateTableCommand(
        schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci");
    var pkTableResult = createTableUseCase.createTable(createPkTableCommand).block().result();
    pkTableId = pkTableResult.tableId();

    // PK Table 컬럼 생성
    var createPkColumn1Command = new CreateColumnCommand(
        pkTableId, "pk_col1", "INT", null, null, null, false, null, null, "PK Column 1");
    var pkColumn1Result = createColumnUseCase.createColumn(createPkColumn1Command).block().result();
    pkColumnId1 = pkColumn1Result.columnId();

    var createPkColumn2Command = new CreateColumnCommand(
        pkTableId, "pk_col2", "INT", null, null, null, false, null, null, "PK Column 2");
    var pkColumn2Result = createColumnUseCase.createColumn(createPkColumn2Command).block().result();
    pkColumnId2 = pkColumn2Result.columnId();

    // PK Constraint 생성 (복합키: pk_col1, pk_col2)
    var createPkConstraintCommand = new CreateConstraintCommand(
        pkTableId, "pk_composite", ConstraintKind.PRIMARY_KEY, null, null,
        List.of(
            new CreateConstraintColumnCommand(pkColumnId1, 0),
            new CreateConstraintColumnCommand(pkColumnId2, 1)));
    var pkConstraintResult = createConstraintUseCase.createConstraint(createPkConstraintCommand).block().result();
    pkConstraintId = pkConstraintResult.constraintId();

    // FK Table 1 생성
    var createFkTable1Command = new CreateTableCommand(
        schemaId, "fk_table_1", "utf8mb4", "utf8mb4_general_ci");
    var fkTable1Result = createTableUseCase.createTable(createFkTable1Command).block().result();
    fkTableId1 = fkTable1Result.tableId();

    // FK Table 2 생성 (다중 Relationship 테스트용)
    var createFkTable2Command = new CreateTableCommand(
        schemaId, "fk_table_2", "utf8mb4", "utf8mb4_general_ci");
    var fkTable2Result = createTableUseCase.createTable(createFkTable2Command).block().result();
    fkTableId2 = fkTable2Result.tableId();
  }

  @Nested
  @DisplayName("PK Constraint 컬럼 제거 시")
  class RemovePkConstraintColumn {

    @Test
    @DisplayName("PK Constraint 컬럼 제거 시 해당 컬럼을 참조하는 RelationshipColumn이 삭제된다")
    void deletesRelationshipColumnWhenPkConstraintColumnRemoved() {
      // Given: Relationship 생성 (pk_col1, pk_col2 모두 참조)
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // PK Constraint에서 pk_col1에 해당하는 ConstraintColumn ID 조회
      var constraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(pkConstraintId))
          .block();
      String constraintColumnIdForPkCol1 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId1))
          .findFirst()
          .orElseThrow()
          .id();

      // When: PK Constraint에서 pk_col1 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol1)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: pk_col1을 참조하던 RelationshipColumn 삭제, pk_col2 참조는 유지
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("PK Constraint의 마지막 컬럼 제거 시 Relationship 자체가 삭제된다")
    void deletesRelationshipWhenLastPkConstraintColumnRemoved() {
      // Given: 단일 PK 컬럼을 참조하는 Relationship 생성
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // PK Constraint에서 pk_col1에 해당하는 ConstraintColumn ID 조회
      var constraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(pkConstraintId))
          .block();
      String constraintColumnIdForPkCol1 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId1))
          .findFirst()
          .orElseThrow()
          .id();
      String constraintColumnIdForPkCol2 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId2))
          .findFirst()
          .orElseThrow()
          .id();

      // When: PK Constraint에서 pk_col1 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol1)))
          .expectNextCount(1)
          .verifyComplete();

      // When: 마지막 PK 컬럼 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol2)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: Relationship 자체가 삭제됨
      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(relationshipId)))
          .expectError(RelationshipNotExistException.class)
          .verify();
    }

    @Test
    @DisplayName("IDENTIFYING Relationship에서 PK 컬럼 제거 시 연쇄 삭제된다")
    void cascadeDeletesIdentifyingRelationshipColumn() {
      // Given: IDENTIFYING Relationship 생성
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // PK Constraint에서 pk_col1에 해당하는 ConstraintColumn ID 조회
      var constraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(pkConstraintId))
          .block();
      String constraintColumnIdForPkCol1 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId1))
          .findFirst()
          .orElseThrow()
          .id();

      // When: PK Constraint에서 pk_col1 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol1)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: pk_col1을 참조하던 RelationshipColumn 삭제
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("NON_IDENTIFYING Relationship에서도 PK 컬럼 제거 시 연쇄 삭제된다")
    void cascadeDeletesNonIdentifyingRelationshipColumn() {
      // Given: NON_IDENTIFYING Relationship 생성
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // PK Constraint에서 pk_col2에 해당하는 ConstraintColumn ID 조회
      var constraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(pkConstraintId))
          .block();
      String constraintColumnIdForPkCol2 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId2))
          .findFirst()
          .orElseThrow()
          .id();

      // When: PK Constraint에서 pk_col2 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol2)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: pk_col2를 참조하던 RelationshipColumn 삭제
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId1);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 Relationship이 같은 PK 컬럼을 참조할 때 모두 영향받는다")
    void affectsAllRelationshipsReferencingSamePkColumn() {
      // Given: 두 개의 Relationship이 같은 PK 테이블을 참조
      var createRelCommand1 = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult1 = createRelationshipUseCase.createRelationship(createRelCommand1).block().result();
      String relationshipId1 = relResult1.relationshipId();

      var createRelCommand2 = new CreateRelationshipCommand(fkTableId2,
          pkTableId,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_ONE);
      var relResult2 = createRelationshipUseCase.createRelationship(createRelCommand2).block().result();
      String relationshipId2 = relResult2.relationshipId();

      // PK Constraint에서 pk_col1에 해당하는 ConstraintColumn ID 조회
      var constraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(pkConstraintId))
          .block();
      String constraintColumnIdForPkCol1 = constraintColumns.stream()
          .filter(cc -> cc.columnId().equals(pkColumnId1))
          .findFirst()
          .orElseThrow()
          .id();

      // When: PK Constraint에서 pk_col1 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(constraintColumnIdForPkCol1)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: 첫 번째 Relationship의 pk_col1 참조 RelationshipColumn 삭제
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId1)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId2);
          })
          .verifyComplete();

      // Then: 두 번째 Relationship의 pk_col1 참조 RelationshipColumn도 삭제
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId2)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId2);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("PK Constraint 컬럼 추가 시")
  class AddPkConstraintColumn {

    @Test
    @DisplayName("PK Constraint 컬럼 추가 시 FK 테이블에 FK 컬럼과 RelationshipColumn이 자동 생성된다")
    void cascateCreatesFkColumnAndRelationshipColumnOnPkAdd() {
      // Given: 기존 Relationship 생성 (PK 컬럼 전체 참조)
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // Given: PK 테이블에 새 컬럼 추가
      var createNewPkColumnCommand = new CreateColumnCommand(
          pkTableId, "pk_col3", "INT", null, null, null, false, null, null, "PK Column 3");
      var newPkColumnResult = createColumnUseCase.createColumn(createNewPkColumnCommand).block().result();
      String newPkColumnId = newPkColumnResult.columnId();

      // When: PK Constraint에 새 컬럼 추가
      StepVerifier.create(addConstraintColumnUseCase.addConstraintColumn(
          new AddConstraintColumnCommand(pkConstraintId, newPkColumnId, 2)))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.columnId()).isEqualTo(newPkColumnId);
            assertThat(payload.cascadeCreatedColumns()).hasSize(1);
            var cascade = payload.cascadeCreatedColumns().get(0);
            assertThat(cascade.fkTableId()).isEqualTo(fkTableId1);
            assertThat(cascade.fkColumnName()).isEqualTo("pk_col3");
            assertThat(cascade.relationshipId()).isEqualTo(relationshipId);
          })
          .verifyComplete();

      // Then: RelationshipColumn이 3개로 증가 (기존 2 + cascade 1)
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns).extracting(c -> c.pkColumnId())
                .containsExactlyInAnyOrder(pkColumnId1, pkColumnId2, newPkColumnId);
          })
          .verifyComplete();

      // Then: FK 테이블에 새 컬럼이 추가됨
      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId1)))
          .assertNext(columns -> {
            assertThat(columns.stream().map(c -> c.name()).toList())
                .contains("pk_col3");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("여러 FK 테이블이 참조할 때 모두 cascade 전파된다")
    void cascadesToMultipleFkTablesOnPkAdd() {
      // Given: 두 FK 테이블이 PK 테이블을 참조하는 Relationship 생성
      var createRelCommand1 = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      createRelationshipUseCase.createRelationship(createRelCommand1).block();

      var createRelCommand2 = new CreateRelationshipCommand(fkTableId2,
          pkTableId,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_ONE);
      createRelationshipUseCase.createRelationship(createRelCommand2).block();

      // Given: PK 테이블에 새 컬럼 추가
      var createNewPkColumnCommand = new CreateColumnCommand(
          pkTableId, "pk_col3", "VARCHAR", 100, null, null, false, "utf8mb4", "utf8mb4_general_ci", "PK Column 3");
      var newPkColumnResult = createColumnUseCase.createColumn(createNewPkColumnCommand).block().result();
      String newPkColumnId = newPkColumnResult.columnId();

      // When: PK Constraint에 새 컬럼 추가
      StepVerifier.create(addConstraintColumnUseCase.addConstraintColumn(
          new AddConstraintColumnCommand(pkConstraintId, newPkColumnId, 2)))
          .assertNext(result -> {
            assertThat(result.result().cascadeCreatedColumns()).hasSize(2);
          })
          .verifyComplete();

      // Then: 두 FK 테이블 모두에 새 컬럼이 추가됨
      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId1)))
          .assertNext(columns -> assertThat(columns.stream().map(c -> c.name()).toList())
              .contains("pk_col3"))
          .verifyComplete();

      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId2)))
          .assertNext(columns -> assertThat(columns.stream().map(c -> c.name()).toList())
              .contains("pk_col3"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("UNIQUE Constraint 컬럼 제거 시")
  class RemoveUniqueConstraintColumn {

    @Test
    @DisplayName("UNIQUE Constraint 컬럼 제거는 Relationship에 영향 없다")
    void removingUniqueConstraintColumnDoesNotAffectRelationship() {
      // Given: UNIQUE Constraint 생성 (pk_col1에 대해)
      var createUniqueConstraintCommand = new CreateConstraintCommand(
          pkTableId, "uq_col1", ConstraintKind.UNIQUE, null, null,
          List.of(new CreateConstraintColumnCommand(pkColumnId1, 0)));
      var uniqueConstraintResult = createConstraintUseCase.createConstraint(
          createUniqueConstraintCommand).block().result();
      String uniqueConstraintId = uniqueConstraintResult.constraintId();

      // Given: Relationship 생성 (PK 컬럼 전체 참조)
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String relationshipId = relResult.relationshipId();

      // UNIQUE Constraint의 ConstraintColumn ID 조회
      var uniqueConstraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(uniqueConstraintId))
          .block();
      String uniqueConstraintColumnId = uniqueConstraintColumns.get(0).id();

      // When: UNIQUE Constraint에서 컬럼 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(uniqueConstraintColumnId)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: Relationship은 그대로 유지됨 (UNIQUE 제약조건 제거는 Relationship에 영향 없음)
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns).extracting(c -> c.pkColumnId())
                .containsExactlyInAnyOrder(pkColumnId1, pkColumnId2);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("PK 컬럼 타입 변경 시 FK 전파")
  class PkColumnTypeChangePropagation {

    @Test
    @DisplayName("PK 컬럼 타입 변경 시 FK 컬럼에도 타입이 전파된다")
    void propagatesTypeChangeToFkColumns() {
      // Given: Relationship 생성 (pk_col1 참조)
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String fkColumnId = getFkColumnId(relResult.relationshipId(), pkColumnId1);

      // When: PK 컬럼 타입을 INT → BIGINT로 변경
      StepVerifier.create(changeColumnTypeUseCase.changeColumnType(
          new ChangeColumnTypeCommand(pkColumnId1, "BIGINT", null, null, null)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: FK 컬럼도 BIGINT로 변경됨
      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId1)))
          .assertNext(columns -> {
            var fkColumn = columns.stream()
                .filter(c -> c.id().equals(fkColumnId))
                .findFirst()
                .orElseThrow();
            assertThat(fkColumn.dataType()).isEqualTo("BIGINT");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다수의 FK 테이블에 타입이 전파된다")
    void propagatesTypeChangeToMultipleFkTables() {
      // Given: 두 FK 테이블이 같은 PK 컬럼을 참조
      var createRelCommand1 = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult1 = createRelationshipUseCase.createRelationship(createRelCommand1).block().result();
      String fkColumnId1 = getFkColumnId(relResult1.relationshipId(), pkColumnId1);

      var createRelCommand2 = new CreateRelationshipCommand(fkTableId2,
          pkTableId,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_ONE);
      var relResult2 = createRelationshipUseCase.createRelationship(createRelCommand2).block().result();
      String fkColumnId2 = getFkColumnId(relResult2.relationshipId(), pkColumnId1);

      // When: PK 컬럼 타입을 INT → BIGINT로 변경
      StepVerifier.create(changeColumnTypeUseCase.changeColumnType(
          new ChangeColumnTypeCommand(pkColumnId1, "BIGINT", null, null, null)))
          .expectNextCount(1)
          .verifyComplete();

      // Then: 두 FK 테이블의 컬럼 모두 BIGINT로 변경됨
      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId1)))
          .assertNext(columns -> {
            var fkColumn = columns.stream()
                .filter(c -> c.id().equals(fkColumnId1))
                .findFirst()
                .orElseThrow();
            assertThat(fkColumn.dataType()).isEqualTo("BIGINT");
          })
          .verifyComplete();

      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId2)))
          .assertNext(columns -> {
            var fkColumn = columns.stream()
                .filter(c -> c.id().equals(fkColumnId2))
                .findFirst()
                .orElseThrow();
            assertThat(fkColumn.dataType()).isEqualTo("BIGINT");
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("PK 컬럼 charset/collation 변경 시 FK 전파")
  class PkColumnMetaChangePropagation {

    @Test
    @DisplayName("PK 컬럼 charset/collation 변경 시 FK 컬럼에도 전파된다")
    void propagatesCharsetCollationToFkColumns() {
      // Given: VARCHAR PK 컬럼과 FK 컬럼 생성
      var createVarcharPkColumnCommand = new CreateColumnCommand(
          pkTableId, "pk_varchar", "VARCHAR", 100, null, null, false, null, null, null);
      var varcharPkResult = createColumnUseCase.createColumn(createVarcharPkColumnCommand).block().result();
      String varcharPkColumnId = varcharPkResult.columnId();

      // PK Constraint에 VARCHAR 컬럼 추가
      addConstraintColumnUseCase.addConstraintColumn(
          new AddConstraintColumnCommand(pkConstraintId, varcharPkColumnId, 2)).block();

      // Relationship 생성
      var createRelCommand = new CreateRelationshipCommand(fkTableId1,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block().result();
      String varcharFkColumnId = getFkColumnId(relResult.relationshipId(), varcharPkColumnId);

      // When: PK 컬럼 charset/collation 변경
      StepVerifier.create(changeColumnMetaUseCase.changeColumnMeta(
          new ChangeColumnMetaCommand(varcharPkColumnId, PatchField.absent(), PatchField.of("utf8mb4"), PatchField.of(
              "utf8mb4_unicode_ci"), PatchField.absent())))
          .expectNextCount(1)
          .verifyComplete();

      // Then: FK 컬럼도 charset/collation이 변경됨
      StepVerifier.create(getColumnsByTableIdUseCase
          .getColumnsByTableId(new GetColumnsByTableIdQuery(fkTableId1)))
          .assertNext(columns -> {
            var fkColumn = columns.stream()
                .filter(c -> c.id().equals(varcharFkColumnId))
                .findFirst()
                .orElseThrow();
            assertThat(fkColumn.charset()).isEqualTo("utf8mb4");
            assertThat(fkColumn.collation()).isEqualTo("utf8mb4_unicode_ci");
          })
          .verifyComplete();
    }

  }

  private String getFkColumnId(String relationshipId, String pkColumnId) {
    var columns = getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(
            new GetRelationshipColumnsByRelationshipIdQuery(relationshipId))
        .block();
    return columns.stream()
        .filter(column -> column.pkColumnId().equals(pkColumnId))
        .findFirst()
        .orElseThrow()
        .fkColumnId();
  }

}
