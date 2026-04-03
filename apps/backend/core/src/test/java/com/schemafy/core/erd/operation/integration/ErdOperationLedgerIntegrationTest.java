package com.schemafy.core.erd.operation.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameUseCase;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeUseCase;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameUseCase;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeUseCase;
import com.schemafy.core.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameUseCase;
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
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
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
  ChangeTableNameUseCase changeTableNameUseCase;

  @Autowired
  DeleteTableUseCase deleteTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  ChangeColumnNameUseCase changeColumnNameUseCase;

  @Autowired
  ChangeColumnTypeUseCase changeColumnTypeUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  ChangeConstraintNameUseCase changeConstraintNameUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  ChangeIndexNameUseCase changeIndexNameUseCase;

  @Autowired
  ChangeIndexTypeUseCase changeIndexTypeUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  ChangeRelationshipNameUseCase changeRelationshipNameUseCase;

  @Autowired
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Autowired
  ChangeRelationshipCardinalityUseCase changeRelationshipCardinalityUseCase;

  @Autowired
  UndoErdOperationUseCase undoErdOperationUseCase;

  @Autowired
  RedoErdOperationUseCase redoErdOperationUseCase;

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

  @Test
  @DisplayName("단순 속성 변경 연산은 inverse payload를 저장한다")
  void storesInversePayloadForSimplePropertyChanges() {
    TestGraph graph = createSimplePropertyGraph("erd_operation_inverse_payload");

    StepVerifier.create(changeTableNameUseCase.changeTableName(
        new ChangeTableNameCommand(graph.fkTableId(), "orders_v2")))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeColumnNameUseCase.changeColumnName(
        new ChangeColumnNameCommand(graph.fkBusinessColumnId(), "status_code")))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeColumnTypeUseCase.changeColumnType(
        new ChangeColumnTypeCommand(graph.fkBusinessColumnId(), "INT", null, null, null)))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeRelationshipNameUseCase.changeRelationshipName(
        new ChangeRelationshipNameCommand(graph.relationshipId(), "rel_orders_v2_to_users_manual")))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeRelationshipKindUseCase.changeRelationshipKind(
        new ChangeRelationshipKindCommand(graph.relationshipId(), RelationshipKind.IDENTIFYING)))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeRelationshipCardinalityUseCase.changeRelationshipCardinality(
        new ChangeRelationshipCardinalityCommand(graph.relationshipId(), Cardinality.ONE_TO_ONE)))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeConstraintNameUseCase.changeConstraintName(
        new ChangeConstraintNameCommand(graph.constraintId(), "uq_orders_v2_status_code")))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeIndexNameUseCase.changeIndexName(
        new ChangeIndexNameCommand(graph.indexId(), "idx_orders_v2_status_code")))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(changeIndexTypeUseCase.changeIndexType(
        new ChangeIndexTypeCommand(graph.indexId(), IndexType.HASH)))
        .expectNextCount(1)
        .verifyComplete();

    assertInversePayload(
        graph.schemaId(),
        "CHANGE_TABLE_NAME",
        ChangeTableNameCommand.class,
        new ChangeTableNameCommand(graph.fkTableId(), "orders"));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_COLUMN_NAME",
        ChangeColumnNameCommand.class,
        new ChangeColumnNameCommand(graph.fkBusinessColumnId(), "order_amount"));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_COLUMN_TYPE",
        ChangeColumnTypeCommand.class,
        new ChangeColumnTypeCommand(graph.fkBusinessColumnId(), "DECIMAL", null, null, null));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_RELATIONSHIP_NAME",
        ChangeRelationshipNameCommand.class,
        new ChangeRelationshipNameCommand(graph.relationshipId(), "rel_orders_v2_to_users"));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_RELATIONSHIP_KIND",
        ChangeRelationshipKindCommand.class,
        new ChangeRelationshipKindCommand(graph.relationshipId(), RelationshipKind.NON_IDENTIFYING));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_RELATIONSHIP_CARDINALITY",
        ChangeRelationshipCardinalityCommand.class,
        new ChangeRelationshipCardinalityCommand(graph.relationshipId(), Cardinality.ONE_TO_MANY));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_CONSTRAINT_NAME",
        ChangeConstraintNameCommand.class,
        new ChangeConstraintNameCommand(graph.constraintId(), "uq_orders_status"));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_INDEX_NAME",
        ChangeIndexNameCommand.class,
        new ChangeIndexNameCommand(graph.indexId(), "idx_orders_status"));
    assertInversePayload(
        graph.schemaId(),
        "CHANGE_INDEX_TYPE",
        ChangeIndexTypeCommand.class,
        new ChangeIndexTypeCommand(graph.indexId(), IndexType.BTREE));
  }

  @Test
  @DisplayName("단순 속성 변경 undo redo는 파생 operation metadata와 함께 수행된다")
  void undoesAndRedoesSimplePropertyChangesWithDerivedMetadata() {
    String projectId = createActiveProjectId("erd_operation_undo_redo");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "undo_redo_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "orders",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String sourceOpId = changeTableNameUseCase.changeTableName(new ChangeTableNameCommand(tableId, "orders_v2"))
        .block()
        .operation()
        .opId();

    StepVerifier.create(undoErdOperationUseCase.undo(
        new UndoErdOperationCommand(sourceOpId)))
        .assertNext(result -> assertThat(result.operation().derivationKind()).isEqualTo(ErdOperationDerivationKind.UNDO))
        .verifyComplete();

    assertThat(tableName(tableId)).isEqualTo("orders");
    assertLatestDerivedOperation(schemaId, "CHANGE_TABLE_NAME", "UNDO", sourceOpId);

    StepVerifier.create(redoErdOperationUseCase.redo(
        new RedoErdOperationCommand(sourceOpId)))
        .assertNext(result -> assertThat(result.operation().derivationKind()).isEqualTo(ErdOperationDerivationKind.REDO))
        .verifyComplete();

    assertThat(tableName(tableId)).isEqualTo("orders_v2");
    assertLatestDerivedOperation(schemaId, "CHANGE_TABLE_NAME", "REDO", sourceOpId);
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

  private String tableName(String tableId) {
    return databaseClient.sql("""
        SELECT name
        FROM db_tables
        WHERE id = :tableId
        """)
        .bind("tableId", tableId)
        .map((row, metadata) -> row.get("name", String.class))
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

  private <T> void assertInversePayload(
      String schemaId,
      String opType,
      Class<T> payloadType,
      T expectedPayload) {
    OperationPayloadRow operationPayloadRow = databaseClient.sql("""
        SELECT CAST(inverse_payload_json AS VARCHAR) AS inverse_payload_json_text
        FROM erd_operation_log
        WHERE schema_id = :schemaId
          AND op_type = :opType
        ORDER BY committed_revision DESC
        LIMIT 1
        """)
        .bind("schemaId", schemaId)
        .bind("opType", opType)
        .map((row, metadata) -> new OperationPayloadRow(row.get("inverse_payload_json_text", String.class)))
        .one()
        .block();

    assertThat(operationPayloadRow.inversePayloadJson()).isNotBlank();
    assertThat(parsePersistedValue(operationPayloadRow.inversePayloadJson(), payloadType))
        .isEqualTo(expectedPayload);
  }

  private void assertLatestDerivedOperation(
      String schemaId,
      String expectedOpType,
      String expectedDerivationKind,
      String expectedDerivedFromOpId) {
    DerivedOperationRow operationRow = databaseClient.sql("""
        SELECT op_type,
               derivation_kind,
               derived_from_op_id
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        ORDER BY committed_revision DESC
        LIMIT 1
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> new DerivedOperationRow(
            row.get("op_type", String.class),
            row.get("derivation_kind", String.class),
            row.get("derived_from_op_id", String.class)))
        .one()
        .block();

    assertThat(operationRow.opType()).isEqualTo(expectedOpType);
    assertThat(operationRow.derivationKind()).isEqualTo(expectedDerivationKind);
    assertThat(operationRow.derivedFromOpId()).isEqualTo(expectedDerivedFromOpId);
  }

  private List<String> parseStringArray(String rawJson) {
    JsonNode node = jsonCodec.parsePersistedNode(rawJson);
    assertThat(node.isArray()).isTrue();
    return java.util.stream.StreamSupport.stream(node.spliterator(), false)
        .map(JsonNode::asText)
        .toList();
  }

  private <T> T parsePersistedValue(String rawJson, Class<T> type) {
    return jsonCodec.parsePersisted(rawJson, type);
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

  private TestGraph createSimplePropertyGraph(String prefix) {
    String projectId = createActiveProjectId(prefix);

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        prefix + "_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String pkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String fkTableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "orders",
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
        null)).block().result().columnId();

    String fkBusinessColumnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        fkTableId,
        "order_amount",
        "DECIMAL",
        null,
        10,
        2,
        false,
        null,
        null,
        null)).block().result().columnId();

    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

    String constraintId = createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        fkTableId,
        "uq_orders_status",
        ConstraintKind.UNIQUE,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(fkBusinessColumnId, 0)))).block().result().constraintId();

    String indexId = createIndexUseCase.createIndex(new CreateIndexCommand(
        fkTableId,
        "idx_orders_status",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(fkBusinessColumnId, 0, SortDirection.ASC)))).block().result().indexId();

    String relationshipId = createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null)).block().result().relationshipId();

    return new TestGraph(schemaId, fkTableId, fkBusinessColumnId, constraintId, indexId, relationshipId);
  }

  private record OperationLogRow(
      String opType,
      long committedRevision,
      String actorUserId,
      String affectedTableIdsJson) {
  }

  private record OperationPayloadRow(
      String inversePayloadJson) {
  }

  private record DerivedOperationRow(
      String opType,
      String derivationKind,
      String derivedFromOpId) {
  }

  private record TestGraph(
      String schemaId,
      String fkTableId,
      String fkBusinessColumnId,
      String constraintId,
      String indexId,
      String relationshipId) {
  }

}
