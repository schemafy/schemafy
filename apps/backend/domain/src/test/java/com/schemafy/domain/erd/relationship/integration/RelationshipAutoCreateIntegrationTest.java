package com.schemafy.domain.erd.relationship.integration;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipColumnsByRelationshipIdUseCase;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Relationship 자동 생성 통합 테스트")
class RelationshipAutoCreateIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @Autowired
  GetRelationshipColumnsByRelationshipIdUseCase getRelationshipColumnsByRelationshipIdUseCase;

  @Autowired
  GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  @Test
  @DisplayName("PK 기준으로 FK 컬럼과 관계 컬럼을 자동 생성한다")
  void createsAutoRelationshipFromPk() {
    String schemaId = createSchema("auto_rel");
    String pkTableId = createTable(schemaId, "pk_table");
    String fkTableId = createTable(schemaId, "fk_table");

    String pkCol1Id = createColumn(pkTableId, "pk_col1", "INT", 0);
    String pkCol2Id = createColumn(pkTableId, "pk_col2", "INT", 1);
    createPrimaryKey(pkTableId, "pk_pk_table", List.of(
        new CreateConstraintColumnCommand(pkCol1Id, 0),
        new CreateConstraintColumnCommand(pkCol2Id, 1)));

    var createCommand = new CreateRelationshipCommand(fkTableId,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
    var result = createRelationshipUseCase.createRelationship(createCommand).block();

    assertThat(result.name()).isEqualTo("rel_fk_table_to_pk_table");

    StepVerifier.create(getColumnsByTableIdUseCase.getColumnsByTableId(
        new GetColumnsByTableIdQuery(fkTableId)))
        .assertNext(columns -> {
          assertThat(columns).hasSize(2);
          assertThat(columns).extracting(Column::name)
              .containsExactlyInAnyOrder("pk_col1", "pk_col2");
        })
        .verifyComplete();

    StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(
            new GetRelationshipColumnsByRelationshipIdQuery(result.relationshipId())))
        .assertNext(columns -> {
          assertThat(columns).hasSize(2);
          assertThat(columns).extracting(RelationshipColumn::seqNo)
              .containsExactlyInAnyOrder(0, 1);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("IDENTIFYING 자동 관계 생성 시 하위 관계로 전파된다")
  void cascadesIdentifyingAutoRelationship() {
    String schemaId = createSchema("auto_rel_cascade");
    String parentTableId = createTable(schemaId, "parent_table");
    String childTableId = createTable(schemaId, "child_table");
    String grandchildTableId = createTable(schemaId, "grandchild_table");

    String parentPkId = createColumn(parentTableId, "parent_id", "INT", 0);
    createPrimaryKey(parentTableId, "pk_parent_table", List.of(
        new CreateConstraintColumnCommand(parentPkId, 0)));

    String childPkId = createColumn(childTableId, "child_id", "INT", 0);
    createPrimaryKey(childTableId, "pk_child_table", List.of(
        new CreateConstraintColumnCommand(childPkId, 0)));

    var childToGrandchild = new CreateRelationshipCommand(grandchildTableId,
          childTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);
    var childRelationship = createRelationshipUseCase.createRelationship(childToGrandchild).block();

    var parentToChild = new CreateRelationshipCommand(childTableId,
          parentTableId,
          RelationshipKind.IDENTIFYING,
          Cardinality.ONE_TO_MANY);
    createRelationshipUseCase.createRelationship(parentToChild).block();

    StepVerifier.create(getColumnsByTableIdUseCase.getColumnsByTableId(
        new GetColumnsByTableIdQuery(grandchildTableId)))
        .assertNext(columns -> {
          assertThat(columns).hasSize(2);
          assertThat(columns).extracting(Column::name).contains("parent_id");
        })
        .verifyComplete();

    StepVerifier.create(getRelationshipColumnsByRelationshipIdUseCase
        .getRelationshipColumnsByRelationshipId(
            new GetRelationshipColumnsByRelationshipIdQuery(childRelationship.relationshipId())))
        .assertNext(columns -> assertThat(columns).hasSize(2))
        .verifyComplete();

    StepVerifier.create(getConstraintsByTableIdUseCase.getConstraintsByTableId(
        new GetConstraintsByTableIdQuery(grandchildTableId)))
        .assertNext(constraints -> assertThat(constraints).isEmpty())
        .verifyComplete();
  }

  @Test
  @DisplayName("PK 제약이 없으면 자동 관계 생성이 실패한다")
  void throwsWhenPkMissing() {
    String schemaId = createSchema("auto_rel_no_pk");
    String pkTableId = createTable(schemaId, "pk_table");
    String fkTableId = createTable(schemaId, "fk_table");
    createColumn(pkTableId, "pk_col1", "INT", 0);

    var createCommand = new CreateRelationshipCommand(fkTableId,
          pkTableId,
          RelationshipKind.NON_IDENTIFYING,
          Cardinality.ONE_TO_MANY);

    StepVerifier.create(createRelationshipUseCase.createRelationship(createCommand))
        .expectError(InvalidValueException.class)
        .verify();
  }

  private String createSchema(String prefix) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", prefix + "_" + uniqueSuffix,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
    return schemaResult.id();
  }

  private String createTable(String schemaId, String name) {
    var createTableCommand = new CreateTableCommand(
        schemaId, name, "utf8mb4", "utf8mb4_general_ci");
    var tableResult = createTableUseCase.createTable(createTableCommand).block();
    return tableResult.tableId();
  }

  private String createColumn(String tableId, String name, String dataType, int seqNo) {
    var createColumnCommand = new CreateColumnCommand(
        tableId, name, dataType, null, null, null, seqNo, false, null, null, null);
    var columnResult = createColumnUseCase.createColumn(createColumnCommand).block();
    return columnResult.columnId();
  }

  private void createPrimaryKey(
      String tableId,
      String name,
      List<CreateConstraintColumnCommand> columns) {
    var createConstraintCommand = new CreateConstraintCommand(
        tableId, name, ConstraintKind.PRIMARY_KEY, null, null, columns);
    createConstraintUseCase.createConstraint(createConstraintCommand).block();
  }
}
