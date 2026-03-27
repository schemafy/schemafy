package com.schemafy.core.erd.operation.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.support.ErdProjectIntegrationSupport;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ERD Operation Ledger 통합 테스트")
class ErdOperationLedgerIntegrationTest extends ErdProjectIntegrationSupport {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  ChangeTableExtraUseCase changeTableExtraUseCase;

  @Autowired
  DeleteTableUseCase deleteTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Autowired
  JsonCodec jsonCodec;

  @Test
  @DisplayName("schema revision과 operation log를 순서대로 기록한다")
  void recordsRevisionAndOperationLogForTopLevelMutations() {
    String projectId = createActiveProjectId("erd_operation_ledger");

    var schemaResult = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "ledger_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result();

    assertThat(currentRevision(schemaResult.id())).isEqualTo(1L);
    assertLastOperation(schemaResult.id(), "CREATE_SCHEMA", 1L, List.of());

    var tableResult = createTableUseCase.createTable(new CreateTableCommand(
        schemaResult.id(),
        "ledger_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        "{\"comment\":\"initial\"}")).block().result();

    assertThat(currentRevision(schemaResult.id())).isEqualTo(2L);
    assertLastOperation(schemaResult.id(), "CREATE_TABLE", 2L, List.of(tableResult.tableId()));

    StepVerifier.create(changeTableExtraUseCase.changeTableExtra(new ChangeTableExtraCommand(
        tableResult.tableId(),
        "{\"comment\":\"updated\"}")))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(currentRevision(schemaResult.id())).isEqualTo(3L);
    assertThat(operationCount(schemaResult.id())).isEqualTo(3L);
    assertLastOperation(schemaResult.id(), "CHANGE_TABLE_EXTRA", 3L, List.of(tableResult.tableId()));
  }

  @Test
  @DisplayName("cascade 삭제에서도 top-level delete만 한 번 기록한다")
  void logsOnlyTopLevelDeleteForCascadeDeletion() {
    String projectId = createActiveProjectId("erd_operation_delete");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "delete_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String pkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "pk_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String fkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "fk_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String pkColumnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        pkTableId,
        "id",
        "INT",
        null,
        null,
        null,
        true,
        null,
        null,
        "pk column")).block().result().columnId();

    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_pk_table",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

    createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null)).block();

    long beforeDeleteCount = operationCount(schemaId);
    long beforeDeleteRevision = currentRevision(schemaId);

    StepVerifier.create(deleteTableUseCase.deleteTable(new DeleteTableCommand(fkTableId)))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(operationCount(schemaId)).isEqualTo(beforeDeleteCount + 1);
    assertThat(currentRevision(schemaId)).isEqualTo(beforeDeleteRevision + 1);
    assertLastOperation(schemaId, "DELETE_TABLE", beforeDeleteRevision + 1, List.of(fkTableId, pkTableId));
  }

  @Test
  @DisplayName("같은 relationship kind 재요청은 revision과 operation log를 늘리지 않는다")
  void doesNotRecordLedgerForNoOpRelationshipKindChange() {
    String projectId = createActiveProjectId("erd_operation_noop_relationship_kind");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "noop_relationship_kind_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String pkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "pk_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String fkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "fk_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String pkColumnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        pkTableId,
        "id",
        "INT",
        null,
        null,
        null,
        true,
        null,
        null,
        "pk column")).block().result().columnId();

    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_pk_table",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

    var relationshipResult = createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null)).block().result();

    long beforeChangeCount = operationCount(schemaId);
    long beforeChangeRevision = currentRevision(schemaId);

    StepVerifier.create(changeRelationshipKindUseCase.changeRelationshipKind(new ChangeRelationshipKindCommand(
        relationshipResult.relationshipId(),
        relationshipResult.kind())))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(operationCount(schemaId)).isEqualTo(beforeChangeCount);
    assertThat(currentRevision(schemaId)).isEqualTo(beforeChangeRevision);
  }

  private long currentRevision(String schemaId) {
    return databaseClient.sql("""
        SELECT current_revision
        FROM schema_collaboration_state
        WHERE schema_id = :schemaId
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> ((Number) row.get("current_revision")).longValue())
        .one()
        .block();
  }

  private long operationCount(String schemaId) {
    return databaseClient.sql("""
        SELECT COUNT(*) AS cnt
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> ((Number) row.get("cnt")).longValue())
        .one()
        .block();
  }

  private void assertLastOperation(
      String schemaId,
      String expectedOpType,
      long expectedRevision,
      List<String> expectedAffectedTableIds) {
    OperationLogRow operationLogRow = databaseClient.sql("""
        SELECT op_type,
               committed_revision,
               actor_user_id,
               CAST(affected_table_ids_json AS VARCHAR) AS affected_table_ids_json_text
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        ORDER BY committed_revision DESC
        LIMIT 1
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> new OperationLogRow(
            row.get("op_type", String.class),
            ((Number) row.get("committed_revision")).longValue(),
            row.get("actor_user_id", String.class),
            row.get("affected_table_ids_json_text", String.class)))
        .one()
        .block();

    assertThat(operationLogRow.opType()).isEqualTo(expectedOpType);
    assertThat(operationLogRow.committedRevision()).isEqualTo(expectedRevision);
    assertThat(operationLogRow.actorUserId()).isEqualTo("system");
    assertThat(parseStringArray(operationLogRow.affectedTableIdsJson()))
        .containsExactlyInAnyOrderElementsOf(expectedAffectedTableIds);
  }

  private List<String> parseStringArray(String rawJson) {
    JsonNode node = jsonCodec.parsePersistedNode(rawJson);
    assertThat(node.isArray()).isTrue();
    return java.util.stream.StreamSupport.stream(node.spliterator(), false)
        .map(JsonNode::asText)
        .toList();
  }

  private record OperationLogRow(
      String opType,
      long committedRevision,
      String actorUserId,
      String affectedTableIdsJson) {
  }

}
