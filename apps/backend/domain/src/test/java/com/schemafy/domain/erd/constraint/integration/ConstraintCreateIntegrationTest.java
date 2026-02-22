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
import org.junit.jupiter.api.TestInfo;

import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintColumnsByConstraintIdUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Constraint 생성 및 관리 통합 테스트")
class ConstraintCreateIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  GetConstraintUseCase getConstraintUseCase;

  @Autowired
  GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  @Autowired
  GetConstraintColumnsByConstraintIdUseCase getConstraintColumnsByConstraintIdUseCase;

  @Autowired
  ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @Autowired
  AddConstraintColumnUseCase addConstraintColumnUseCase;

  @Autowired
  ChangeConstraintColumnPositionUseCase changeConstraintColumnPositionUseCase;

  @Autowired
  DeleteConstraintUseCase deleteConstraintUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;
  private String tableId;
  private String columnId1;
  private String columnId2;
  private String columnId3;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "constraint_create_" + uniqueSuffix;

    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block().result();
    schemaId = schemaResult.id();

    var createTableCommand = new CreateTableCommand(
        schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci", null);
    var tableResult = createTableUseCase.createTable(createTableCommand).block().result();
    tableId = tableResult.tableId();

    var createColumn1Command = new CreateColumnCommand(
        tableId, "id", "INT", null, null, null, true, null, null, "PK");
    var column1Result = createColumnUseCase.createColumn(createColumn1Command).block().result();
    columnId1 = column1Result.columnId();

    var createColumn2Command = new CreateColumnCommand(
        tableId, "name", "VARCHAR", 100, null, null, false, null, null, "Name");
    var column2Result = createColumnUseCase.createColumn(createColumn2Command).block().result();
    columnId2 = column2Result.columnId();

    var createColumn3Command = new CreateColumnCommand(
        tableId, "email", "VARCHAR", 255, null, null, false, null, null, "Email");
    var column3Result = createColumnUseCase.createColumn(createColumn3Command).block().result();
    columnId3 = column3Result.columnId();
  }

  @Nested
  @DisplayName("제약조건 생성 시")
  class CreateConstraint {

    @Test
    @DisplayName("PRIMARY KEY 제약조건을 생성한다")
    void createsPrimaryKeyConstraint() {
      var createCommand = new CreateConstraintCommand(
          tableId, "pk_test", ConstraintKind.PRIMARY_KEY, null, null,
          List.of(new CreateConstraintColumnCommand(columnId1, 0)));

      StepVerifier.create(createConstraintUseCase.createConstraint(createCommand))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.constraintId()).isNotNull();
            assertThat(payload.name()).isEqualTo("pk_test");
            assertThat(payload.kind()).isEqualTo(ConstraintKind.PRIMARY_KEY);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("UNIQUE 제약조건을 생성한다")
    void createsUniqueConstraint() {
      var createCommand = new CreateConstraintCommand(
          tableId, "uq_email", ConstraintKind.UNIQUE, null, null,
          List.of(new CreateConstraintColumnCommand(columnId3, 0)));

      StepVerifier.create(createConstraintUseCase.createConstraint(createCommand))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.constraintId()).isNotNull();
            assertThat(payload.kind()).isEqualTo(ConstraintKind.UNIQUE);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("CHECK 제약조건을 checkExpr와 함께 생성한다")
    void createsCheckConstraintWithExpression() {
      var createCommand = new CreateConstraintCommand(
          tableId, "ck_name_length", ConstraintKind.CHECK, "LENGTH(name) >= 2", null,
          List.of(new CreateConstraintColumnCommand(columnId2, 0)));

      StepVerifier.create(createConstraintUseCase.createConstraint(createCommand))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.constraintId()).isNotNull();
            assertThat(payload.kind()).isEqualTo(ConstraintKind.CHECK);
          })
          .verifyComplete();

      // 생성된 제약조건 조회 및 checkExpr 확인
      var constraints = getConstraintsByTableIdUseCase.getConstraintsByTableId(
          new GetConstraintsByTableIdQuery(tableId)).block();
      var checkConstraint = constraints.stream()
          .filter(c -> c.kind() == ConstraintKind.CHECK)
          .findFirst()
          .orElseThrow();
      assertThat(checkConstraint.checkExpr()).isEqualTo("LENGTH(name) >= 2");
    }

    @Test
    @DisplayName("복합 컬럼 제약조건을 생성한다")
    void createsCompositeConstraint() {
      var createCommand = new CreateConstraintCommand(
          tableId, "uq_name_email", ConstraintKind.UNIQUE, null, null,
          List.of(
              new CreateConstraintColumnCommand(columnId2, 0),
              new CreateConstraintColumnCommand(columnId3, 1)));

      var result = createConstraintUseCase.createConstraint(createCommand).block().result();

      StepVerifier.create(getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(result.constraintId())))
          .assertNext(columns -> {
            assertThat(columns).hasSize(2);
            assertThat(columns.get(0).columnId()).isEqualTo(columnId2);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).columnId()).isEqualTo(columnId3);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("제약조건 이름 변경 시")
  class ChangeConstraintName {

    @Test
    @DisplayName("제약조건 이름을 변경한다")
    void changesConstraintName() {
      var createCommand = new CreateConstraintCommand(
          tableId, "pk_old", ConstraintKind.PRIMARY_KEY, null, null,
          List.of(new CreateConstraintColumnCommand(columnId1, 0)));
      var result = createConstraintUseCase.createConstraint(createCommand).block().result();

      StepVerifier.create(changeConstraintNameUseCase.changeConstraintName(
          new ChangeConstraintNameCommand(result.constraintId(), "pk_new")))
          .expectNextCount(1)
          .verifyComplete();

      StepVerifier.create(getConstraintUseCase.getConstraint(
          new GetConstraintQuery(result.constraintId())))
          .assertNext(constraint -> assertThat(constraint.name()).isEqualTo("pk_new"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("제약조건 컬럼 추가 시")
  class AddConstraintColumn {

    @Test
    @DisplayName("기존 제약조건에 컬럼을 추가한다")
    void addsColumnToConstraint() {
      var createCommand = new CreateConstraintCommand(
          tableId, "uq_test", ConstraintKind.UNIQUE, null, null,
          List.of(new CreateConstraintColumnCommand(columnId1, 0)));
      var result = createConstraintUseCase.createConstraint(createCommand).block().result();

      StepVerifier.create(addConstraintColumnUseCase.addConstraintColumn(
          new AddConstraintColumnCommand(result.constraintId(), columnId2, 1)))
          .assertNext(addResult -> {
            var payload = addResult.result();
            assertThat(payload.constraintColumnId()).isNotNull();
            assertThat(payload.columnId()).isEqualTo(columnId2);
            assertThat(payload.seqNo()).isEqualTo(1);
          })
          .verifyComplete();

      StepVerifier.create(getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(result.constraintId())))
          .assertNext(columns -> assertThat(columns).hasSize(2))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("제약조건 컬럼 위치 변경 시")
  class ChangeConstraintColumnPosition {

    @Test
    @DisplayName("제약조건 컬럼의 위치를 변경한다")
    void changesColumnPosition() {
      var createCommand = new CreateConstraintCommand(
          tableId, "uq_composite", ConstraintKind.UNIQUE, null, null,
          List.of(
              new CreateConstraintColumnCommand(columnId1, 0),
              new CreateConstraintColumnCommand(columnId2, 1),
              new CreateConstraintColumnCommand(columnId3, 2)));
      var result = createConstraintUseCase.createConstraint(createCommand).block().result();

      // 현재 컬럼 조회
      var columns = getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(result.constraintId()))
          .block();
      assertThat(columns).hasSize(3);

      // 마지막 컬럼(seqNo 2)을 첫 번째(seqNo 0)로 이동
      String lastColumnId = columns.get(2).id();

      StepVerifier.create(changeConstraintColumnPositionUseCase.changeConstraintColumnPosition(
          new ChangeConstraintColumnPositionCommand(lastColumnId, 0)))
          .expectNextCount(1)
          .verifyComplete();

      // 위치 확인
      StepVerifier.create(getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(result.constraintId())))
          .assertNext(reorderedColumns -> {
            assertThat(reorderedColumns).hasSize(3);
            assertThat(reorderedColumns.get(0).columnId()).isEqualTo(columnId3);
            assertThat(reorderedColumns.get(0).seqNo()).isEqualTo(0);
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("제약조건 삭제 시")
  class DeleteConstraint {

    @Test
    @DisplayName("제약조건과 관련 컬럼을 모두 삭제한다")
    void deletesConstraintAndColumns() {
      var createCommand = new CreateConstraintCommand(
          tableId, "uq_test", ConstraintKind.UNIQUE, null, null,
          List.of(
              new CreateConstraintColumnCommand(columnId1, 0),
              new CreateConstraintColumnCommand(columnId2, 1)));
      var result = createConstraintUseCase.createConstraint(createCommand).block().result();

      StepVerifier.create(deleteConstraintUseCase.deleteConstraint(
          new DeleteConstraintCommand(result.constraintId())))
          .expectNextCount(1)
          .verifyComplete();

      StepVerifier.create(getConstraintUseCase.getConstraint(
          new GetConstraintQuery(result.constraintId())))
          .expectError(ConstraintNotExistException.class)
          .verify();

      StepVerifier.create(getConstraintColumnsByConstraintIdUseCase
          .getConstraintColumnsByConstraintId(
              new GetConstraintColumnsByConstraintIdQuery(result.constraintId())))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

  }

}
