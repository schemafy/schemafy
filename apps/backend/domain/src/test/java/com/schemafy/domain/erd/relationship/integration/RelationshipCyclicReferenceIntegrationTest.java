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
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.domain.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.domain.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.domain.erd.table.application.port.in.CreateTableUseCase;

import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Relationship 순환 참조 검증 통합 테스트")
class RelationshipCyclicReferenceIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  private String schemaId;
  private String tableAId;
  private String tableBId;
  private String tableCId;
  private String columnAId;
  private String columnBId;
  private String columnCId;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String schemaName = "cyclic_" + uniqueSuffix;

    var createSchemaCommand = new CreateSchemaCommand(
        PROJECT_ID, "MySQL", schemaName,
        "utf8mb4", "utf8mb4_general_ci");
    var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
    schemaId = schemaResult.id();

    var createTableACommand = new CreateTableCommand(
        schemaId, "table_a", "utf8mb4", "utf8mb4_general_ci");
    var tableAResult = createTableUseCase.createTable(createTableACommand).block();
    tableAId = tableAResult.tableId();

    var createTableBCommand = new CreateTableCommand(
        schemaId, "table_b", "utf8mb4", "utf8mb4_general_ci");
    var tableBResult = createTableUseCase.createTable(createTableBCommand).block();
    tableBId = tableBResult.tableId();

    var createTableCCommand = new CreateTableCommand(
        schemaId, "table_c", "utf8mb4", "utf8mb4_general_ci");
    var tableCResult = createTableUseCase.createTable(createTableCCommand).block();
    tableCId = tableCResult.tableId();

    var createColumnACommand = new CreateColumnCommand(
        tableAId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var columnAResult = createColumnUseCase.createColumn(createColumnACommand).block();
    columnAId = columnAResult.columnId();

    var createColumnBCommand = new CreateColumnCommand(
        tableBId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var columnBResult = createColumnUseCase.createColumn(createColumnBCommand).block();
    columnBId = columnBResult.columnId();

    var createColumnCCommand = new CreateColumnCommand(
        tableCId, "id", "INT", null, null, null, 0, true, null, null, "PK");
    var columnCResult = createColumnUseCase.createColumn(createColumnCCommand).block();
    columnCId = columnCResult.columnId();
  }

  @Nested
  @DisplayName("IDENTIFYING 관계 생성 시")
  class CreateIdentifyingRelationship {

    @Test
    @DisplayName("직접 순환 참조가 발생하면 예외를 던진다 (A -> B -> A)")
    void throwsOnDirectCyclicReference() {
      // A -> B (IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> A (IDENTIFYING) - 순환 참조!
      var createBACommand = new CreateRelationshipCommand(
          tableBId, tableAId, "fk_b_a",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnBId, 0)));

      StepVerifier.create(createRelationshipUseCase.createRelationship(createBACommand))
          .expectError(RelationshipCyclicReferenceException.class)
          .verify();
    }

    @Test
    @DisplayName("간접 순환 참조가 발생하면 예외를 던진다 (A -> B -> C -> A)")
    void throwsOnIndirectCyclicReference() {
      // A -> B (IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> C (IDENTIFYING)
      var createBCCommand = new CreateRelationshipCommand(
          tableBId, tableCId, "fk_b_c",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnCId, columnBId, 0)));
      createRelationshipUseCase.createRelationship(createBCCommand).block();

      // C -> A (IDENTIFYING) - 간접 순환 참조!
      var createCACommand = new CreateRelationshipCommand(
          tableCId, tableAId, "fk_c_a",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnCId, 0)));

      StepVerifier.create(createRelationshipUseCase.createRelationship(createCACommand))
          .expectError(RelationshipCyclicReferenceException.class)
          .verify();
    }

    @Test
    @DisplayName("NON_IDENTIFYING 관계는 순환 참조를 허용한다")
    void allowsCyclicReferenceForNonIdentifying() {
      // A -> B (NON_IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> A (NON_IDENTIFYING) - 허용됨
      var createBACommand = new CreateRelationshipCommand(
          tableBId, tableAId, "fk_b_a",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnBId, 0)));

      StepVerifier.create(createRelationshipUseCase.createRelationship(createBACommand))
          .expectNextCount(1)
          .verifyComplete();
    }

    @Test
    @DisplayName("혼합 관계에서 IDENTIFYING만 순환 검사에 포함된다")
    void checksOnlyIdentifyingRelationshipsForCycle() {
      // A -> B (NON_IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> A (IDENTIFYING) - NON_IDENTIFYING은 그래프에 없으므로 허용됨
      var createBACommand = new CreateRelationshipCommand(
          tableBId, tableAId, "fk_b_a",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnBId, 0)));

      StepVerifier.create(createRelationshipUseCase.createRelationship(createBACommand))
          .expectNextCount(1)
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("관계 Kind 변경 시")
  class ChangeRelationshipKind {

    @Test
    @DisplayName("NON_IDENTIFYING에서 IDENTIFYING로 변경 시 순환 참조가 발생하면 예외를 던진다")
    void throwsOnCyclicReferenceWhenChangingToIdentifying() {
      // A -> B (NON_IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      var abResult = createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> A (IDENTIFYING)
      var createBACommand = new CreateRelationshipCommand(
          tableBId, tableAId, "fk_b_a",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnBId, 0)));
      createRelationshipUseCase.createRelationship(createBACommand).block();

      // A -> B를 IDENTIFYING로 변경 - 순환 참조!
      var changeKindCommand = new ChangeRelationshipKindCommand(
          abResult.relationshipId(), RelationshipKind.IDENTIFYING);

      StepVerifier.create(changeRelationshipKindUseCase.changeRelationshipKind(changeKindCommand))
          .expectError(RelationshipCyclicReferenceException.class)
          .verify();
    }

    @Test
    @DisplayName("IDENTIFYING에서 NON_IDENTIFYING로 변경은 항상 허용된다")
    void allowsChangingFromIdentifyingToNonIdentifying() {
      // A -> B (IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      var abResult = createRelationshipUseCase.createRelationship(createABCommand).block();

      // A -> B를 NON_IDENTIFYING로 변경
      var changeKindCommand = new ChangeRelationshipKindCommand(
          abResult.relationshipId(), RelationshipKind.NON_IDENTIFYING);

      StepVerifier.create(changeRelationshipKindUseCase.changeRelationshipKind(changeKindCommand))
          .verifyComplete();
    }

    @Test
    @DisplayName("간접 순환 참조도 Kind 변경 시 감지한다")
    void detectsIndirectCycleWhenChangingKind() {
      // A -> B (IDENTIFYING)
      var createABCommand = new CreateRelationshipCommand(
          tableAId, tableBId, "fk_a_b",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnBId, columnAId, 0)));
      createRelationshipUseCase.createRelationship(createABCommand).block();

      // B -> C (IDENTIFYING)
      var createBCCommand = new CreateRelationshipCommand(
          tableBId, tableCId, "fk_b_c",
          RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnCId, columnBId, 0)));
      createRelationshipUseCase.createRelationship(createBCCommand).block();

      // C -> A (NON_IDENTIFYING)
      var createCACommand = new CreateRelationshipCommand(
          tableCId, tableAId, "fk_c_a",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(columnAId, columnCId, 0)));
      var caResult = createRelationshipUseCase.createRelationship(createCACommand).block();

      // C -> A를 IDENTIFYING로 변경 - 간접 순환 참조!
      var changeKindCommand = new ChangeRelationshipKindCommand(
          caResult.relationshipId(), RelationshipKind.IDENTIFYING);

      StepVerifier.create(changeRelationshipKindUseCase.changeRelationshipKind(changeKindCommand))
          .expectError(RelationshipCyclicReferenceException.class)
          .verify();
    }

  }

}
