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

import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
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
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PK Constraint 컬럼 변경이 Relationship에 미치는 영향 테스트
 */
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
  GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  @Autowired
  GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;

  // PK Table (참조되는 테이블)
  private String pkTableId;
  private String pkColumnId1;
  private String pkColumnId2;
  private String pkConstraintId;

  // FK Table 1 (참조하는 테이블)
  private String fkTableId1;
  private String fkColumnId1_1;
  private String fkColumnId1_2;

  // FK Table 2 (참조하는 테이블 - 다중 Relationship 테스트용)
  private String fkTableId2;
  private String fkColumnId2_1;
  private String fkColumnId2_2;

  @BeforeEach
  void setUp() {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "constraint_rel_" + uniqueSuffix;

    // Schema 생성
    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
    schemaId = schemaResult.id();

    // PK Table 생성 (복합키를 가질 테이블)
    var createPkTableCommand = new CreateTableCommand(
        schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci");
    var pkTableResult = createTableUseCase.createTable(createPkTableCommand).block();
    pkTableId = pkTableResult.tableId();

    // PK Table 컬럼 생성
    var createPkColumn1Command = new CreateColumnCommand(
        pkTableId, "pk_col1", "INT", null, null, null, 0, false, null, null, "PK Column 1");
    var pkColumn1Result = createColumnUseCase.createColumn(createPkColumn1Command).block();
    pkColumnId1 = pkColumn1Result.columnId();

    var createPkColumn2Command = new CreateColumnCommand(
        pkTableId, "pk_col2", "INT", null, null, null, 1, false, null, null, "PK Column 2");
    var pkColumn2Result = createColumnUseCase.createColumn(createPkColumn2Command).block();
    pkColumnId2 = pkColumn2Result.columnId();

    // PK Constraint 생성 (복합키: pk_col1, pk_col2)
    var createPkConstraintCommand = new CreateConstraintCommand(
        pkTableId, "pk_composite", ConstraintKind.PRIMARY_KEY, null, null,
        List.of(
            new CreateConstraintColumnCommand(pkColumnId1, 0),
            new CreateConstraintColumnCommand(pkColumnId2, 1)));
    var pkConstraintResult = createConstraintUseCase.createConstraint(createPkConstraintCommand).block();
    pkConstraintId = pkConstraintResult.constraintId();

    // FK Table 1 생성
    var createFkTable1Command = new CreateTableCommand(
        schemaId, "fk_table_1", "utf8mb4", "utf8mb4_general_ci");
    var fkTable1Result = createTableUseCase.createTable(createFkTable1Command).block();
    fkTableId1 = fkTable1Result.tableId();

    // FK Table 1 컬럼 생성
    var createFkColumn1_1Command = new CreateColumnCommand(
        fkTableId1, "fk_col1", "INT", null, null, null, 0, false, null, null, "FK Column 1");
    var fkColumn1_1Result = createColumnUseCase.createColumn(createFkColumn1_1Command).block();
    fkColumnId1_1 = fkColumn1_1Result.columnId();

    var createFkColumn1_2Command = new CreateColumnCommand(
        fkTableId1, "fk_col2", "INT", null, null, null, 1, false, null, null, "FK Column 2");
    var fkColumn1_2Result = createColumnUseCase.createColumn(createFkColumn1_2Command).block();
    fkColumnId1_2 = fkColumn1_2Result.columnId();

    // FK Table 2 생성 (다중 Relationship 테스트용)
    var createFkTable2Command = new CreateTableCommand(
        schemaId, "fk_table_2", "utf8mb4", "utf8mb4_general_ci");
    var fkTable2Result = createTableUseCase.createTable(createFkTable2Command).block();
    fkTableId2 = fkTable2Result.tableId();

    // FK Table 2 컬럼 생성
    var createFkColumn2_1Command = new CreateColumnCommand(
        fkTableId2, "ref_col1", "INT", null, null, null, 0, false, null, null, "Ref Column 1");
    var fkColumn2_1Result = createColumnUseCase.createColumn(createFkColumn2_1Command).block();
    fkColumnId2_1 = fkColumn2_1Result.columnId();

    var createFkColumn2_2Command = new CreateColumnCommand(
        fkTableId2, "ref_col2", "INT", null, null, null, 1, false, null, null, "Ref Column 2");
    var fkColumn2_2Result = createColumnUseCase.createColumn(createFkColumn2_2Command).block();
    fkColumnId2_2 = fkColumn2_2Result.columnId();
  }

  @Nested
  @DisplayName("PK Constraint 컬럼 제거 시")
  class RemovePkConstraintColumn {

    @Test
    @DisplayName("PK Constraint 컬럼 제거 시 해당 컬럼을 참조하는 RelationshipColumn이 삭제된다")
    void deletesRelationshipColumnWhenPkConstraintColumnRemoved() {
      // Given: Relationship 생성 (pk_col1, pk_col2 모두 참조)
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_composite",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0),
              new CreateRelationshipColumnCommand(pkColumnId2, fkColumnId1_2, 1)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
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
          new RemoveConstraintColumnCommand(pkConstraintId, constraintColumnIdForPkCol1)))
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
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_single",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
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
          new RemoveConstraintColumnCommand(pkConstraintId, constraintColumnIdForPkCol1)))
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
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_identifying",
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0),
              new CreateRelationshipColumnCommand(pkColumnId2, fkColumnId1_2, 1)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
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
          new RemoveConstraintColumnCommand(pkConstraintId, constraintColumnIdForPkCol1)))
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
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_non_identifying",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0),
              new CreateRelationshipColumnCommand(pkColumnId2, fkColumnId1_2, 1)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
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
          new RemoveConstraintColumnCommand(pkConstraintId, constraintColumnIdForPkCol2)))
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
      var createRelCommand1 = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_to_table1",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0),
              new CreateRelationshipColumnCommand(pkColumnId2, fkColumnId1_2, 1)));
      var relResult1 = createRelationshipUseCase.createRelationship(createRelCommand1).block();
      String relationshipId1 = relResult1.relationshipId();

      var createRelCommand2 = new CreateRelationshipCommand(
          fkTableId2,
          pkTableId,
          "fk_to_table2",
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_ONE,
          null,
          List.of(
              new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId2_1, 0),
              new CreateRelationshipColumnCommand(pkColumnId2, fkColumnId2_2, 1)));
      var relResult2 = createRelationshipUseCase.createRelationship(createRelCommand2).block();
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
          new RemoveConstraintColumnCommand(pkConstraintId, constraintColumnIdForPkCol1)))
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
    @DisplayName("PK Constraint 컬럼 추가는 기존 Relationship에 영향 없다")
    void addingPkConstraintColumnDoesNotAffectExistingRelationship() {
      // Given: 기존 Relationship이 pk_col1만 참조
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_existing",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
      String relationshipId = relResult.relationshipId();

      // Given: PK 테이블에 새 컬럼 추가
      var createNewPkColumnCommand = new CreateColumnCommand(
          pkTableId, "pk_col3", "INT", null, null, null, 2, false, null, null, "PK Column 3");
      var newPkColumnResult = createColumnUseCase.createColumn(createNewPkColumnCommand).block();
      String newPkColumnId = newPkColumnResult.columnId();

      // When: PK Constraint에 새 컬럼 추가
      StepVerifier.create(addConstraintColumnUseCase.addConstraintColumn(
          new AddConstraintColumnCommand(pkConstraintId, newPkColumnId, 2)))
          .assertNext(result -> assertThat(result.columnId()).isEqualTo(newPkColumnId))
          .verifyComplete();

      // Then: 기존 Relationship은 그대로 유지 (새 컬럼은 명시적으로 매핑해야 함)
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId1);
          })
          .verifyComplete();

      // Then: Relationship 자체도 그대로 유지
      StepVerifier.create(getRelationshipUseCase.getRelationship(
          new GetRelationshipQuery(relationshipId)))
          .assertNext(rel -> assertThat(rel.id()).isEqualTo(relationshipId))
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
          createUniqueConstraintCommand).block();
      String uniqueConstraintId = uniqueConstraintResult.constraintId();

      // Given: Relationship 생성 (pk_col1 참조)
      var createRelCommand = new CreateRelationshipCommand(
          fkTableId1,
          pkTableId,
          "fk_to_pk",
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY,
          null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId1, fkColumnId1_1, 0)));
      var relResult = createRelationshipUseCase.createRelationship(createRelCommand).block();
      String relationshipId = relResult.relationshipId();

      // UNIQUE Constraint의 ConstraintColumn ID 조회
      var uniqueConstraintColumns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(uniqueConstraintId))
          .block();
      String uniqueConstraintColumnId = uniqueConstraintColumns.get(0).id();

      // When: UNIQUE Constraint에서 컬럼 제거
      StepVerifier.create(removeConstraintColumnUseCase.removeConstraintColumn(
          new RemoveConstraintColumnCommand(uniqueConstraintId, uniqueConstraintColumnId)))
          .verifyComplete();

      // Then: Relationship은 그대로 유지됨 (UNIQUE 제약조건 제거는 Relationship에 영향 없음)
      StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
          .getRelationshipColumnsByRelationshipId(
              new GetRelationshipColumnsByRelationshipIdQuery(relationshipId)))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).pkColumnId()).isEqualTo(pkColumnId1);
          })
          .verifyComplete();
    }

  }

}
