package com.schemafy.core.erd.operation.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameUseCase;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.support.ErdProjectIntegrationSupport;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableUseCase;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ERD Operation Ledger 통합 테스트")
class ErdOperationLedgerIntegrationTest extends ErdProjectIntegrationSupport {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  ChangeSchemaNameUseCase changeSchemaNameUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  ChangeTableExtraUseCase changeTableExtraUseCase;

  @Autowired
  DeleteTableUseCase deleteTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Autowired
  JsonCodec jsonCodec;

  @Autowired
  IncrementSchemaCollaborationRevisionPort incrementSchemaCollaborationRevisionPort;

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
  @DisplayName("컬럼 category 전환은 CHANGE_COLUMN_TYPE 한 번만 기록한다")
  void recordsSingleOperationForColumnTypeCategoryTransition() {
    String projectId = createActiveProjectId("erd_operation_single_change_column_type");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "single_change_column_type_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "single_change_column_type_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String columnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        tableId,
        "name",
        "VARCHAR",
        255,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().columnId();

    long beforeRevision = currentRevision(schemaId);
    long beforeCount = operationCount(schemaId);

    StepVerifier.create(changeColumnTypeUseCase.changeColumnType(
        new ChangeColumnTypeCommand(columnId, "INT", null, null, null)))
        .expectNextCount(1)
        .verifyComplete();

    assertThat(currentRevision(schemaId)).isEqualTo(beforeRevision + 1);
    assertThat(operationCount(schemaId)).isEqualTo(beforeCount + 1);
    assertLastOperation(schemaId, "CHANGE_COLUMN_TYPE", beforeRevision + 1, List.of(tableId));
    assertColumnMeta(columnId, null, null);
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

  @Test
  @DisplayName("같은 schema에 대한 concurrent revision increment는 유실 없이 누적된다")
  void incrementsRevisionAtomicallyUnderConcurrency() {
    String projectId = createActiveProjectId("erd_operation_atomic_increment");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "atomic_increment_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    long beforeRevision = currentRevision(schemaId);
    int incrementCount = 20;

    StepVerifier.create(Flux.range(0, incrementCount)
        .flatMap(i -> incrementSchemaCollaborationRevisionPort.increment(schemaId)
            .subscribeOn(Schedulers.parallel()), incrementCount)
        .collectList())
        .assertNext(states -> assertThat(states).hasSize(incrementCount))
        .verifyComplete();

    assertThat(currentRevision(schemaId)).isEqualTo(beforeRevision + incrementCount);
  }

  @Test
  @DisplayName("같은 schema에 대한 병렬 mutation은 모두 성공하고 연속 revision으로 기록된다")
  void recordsAdjacentRevisionsForParallelMutationsOnSameSchema() {
    String projectId = createActiveProjectId("erd_operation_parallel_mutation");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "parallel_mutation_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "parallel_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    long beforeRevision = currentRevision(schemaId);
    long beforeCount = operationCount(schemaId);

    Mono<?> changeSchemaName = Mono.defer(() -> changeSchemaNameUseCase.changeSchemaName(
        new ChangeSchemaNameCommand(schemaId, "parallel_mutation_schema_renamed")))
        .subscribeOn(Schedulers.parallel());

    Mono<?> changeTableExtra = Mono.defer(() -> changeTableExtraUseCase.changeTableExtra(
        new ChangeTableExtraCommand(tableId, "{\"comment\":\"parallel\"}")))
        .subscribeOn(Schedulers.parallel());

    StepVerifier.create(Flux.merge(changeSchemaName, changeTableExtra).collectList())
        .assertNext(results -> assertThat(results).hasSize(2))
        .verifyComplete();

    assertThat(currentRevision(schemaId)).isEqualTo(beforeRevision + 2);
    assertThat(operationCount(schemaId)).isEqualTo(beforeCount + 2);
    assertThat(recentOperationRevisions(schemaId, 2))
        .containsExactly(beforeRevision + 1, beforeRevision + 2);
    assertThat(recentOperationTypes(schemaId, 2))
        .containsExactlyInAnyOrder("CHANGE_SCHEMA_NAME", "CHANGE_TABLE_EXTRA");
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

  private void assertColumnMeta(String columnId, String expectedCharset, String expectedCollation) {
    ColumnMetaRow columnMetaRow = databaseClient.sql("""
        SELECT charset, collation
        FROM db_columns
        WHERE id = :columnId
        """)
        .bind("columnId", columnId)
        .map((row, metadata) -> new ColumnMetaRow(
            row.get("charset", String.class),
            row.get("collation", String.class)))
        .one()
        .block();

    assertThat(columnMetaRow.charset()).isEqualTo(expectedCharset);
    assertThat(columnMetaRow.collation()).isEqualTo(expectedCollation);
  }

  private List<String> parseStringArray(String rawJson) {
    JsonNode node = jsonCodec.parsePersistedNode(rawJson);
    assertThat(node.isArray()).isTrue();
    return java.util.stream.StreamSupport.stream(node.spliterator(), false)
        .map(JsonNode::asText)
        .toList();
  }

  private List<Long> recentOperationRevisions(String schemaId, int limit) {
    return databaseClient.sql("""
        SELECT committed_revision
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        ORDER BY committed_revision DESC
        LIMIT %d
        """.formatted(limit))
        .bind("schemaId", schemaId)
        .map((row, metadata) -> ((Number) row.get("committed_revision")).longValue())
        .all()
        .collectList()
        .map(revisions -> revisions.stream()
            .sorted()
            .toList())
        .block();
  }

  private List<String> recentOperationTypes(String schemaId, int limit) {
    return databaseClient.sql("""
        SELECT op_type
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        ORDER BY committed_revision DESC
        LIMIT %d
        """.formatted(limit))
        .bind("schemaId", schemaId)
        .map((row, metadata) -> row.get("op_type", String.class))
        .all()
        .collectList()
        .block();
  }

  private record OperationLogRow(
      String opType,
      long committedRevision,
      String actorUserId,
      String affectedTableIdsJson) {
  }

  private record ColumnMetaRow(String charset, String collation) {
  }

}
