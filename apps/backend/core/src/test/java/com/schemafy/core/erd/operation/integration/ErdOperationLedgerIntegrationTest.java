package com.schemafy.core.erd.operation.integration;

import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindUseCase;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnUseCase;
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
import com.schemafy.core.ulid.application.service.UlidGenerator;

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
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  RemoveIndexColumnUseCase removeIndexColumnUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  RemoveRelationshipColumnUseCase removeRelationshipColumnUseCase;

  @Autowired
  ChangeRelationshipKindUseCase changeRelationshipKindUseCase;

  @Autowired
  JsonCodec jsonCodec;

  @Autowired
  IncrementSchemaCollaborationRevisionPort incrementSchemaCollaborationRevisionPort;

  @Autowired
  StructuralSnapshotService structuralSnapshotService;

  @Autowired
  UndoErdOperationUseCase undoErdOperationUseCase;

  @Autowired
  RedoErdOperationUseCase redoErdOperationUseCase;

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
  @DisplayName("PK 쪽 table rename으로 관계 이름이 함께 바뀌면 undo/redo affectedTableIds에 양쪽 테이블을 포함한다")
  void tableRenameUndoRedoIncludesRelationshipTablesWhenPkSideNameChanges() {
    assertTableRenameUndoRedoIncludesRelationshipTables(true);
  }

  @Test
  @DisplayName("FK 쪽 table rename으로 관계 이름이 함께 바뀌면 undo/redo affectedTableIds에 양쪽 테이블을 포함한다")
  void tableRenameUndoRedoIncludesRelationshipTablesWhenFkSideNameChanges() {
    assertTableRenameUndoRedoIncludesRelationshipTables(false);
  }

  private void assertTableRenameUndoRedoIncludesRelationshipTables(boolean renamePkSide) {
    String sidePrefix = renamePkSide ? "pk" : "fk";
    String projectId = createActiveProjectId("erd_operation_table_rename_" + sidePrefix + "_relationship_affected");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "table_rename_" + sidePrefix + "_relationship_affected_schema",
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
        "pk column")).block().result().columnId();

    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_users",
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

    String targetTableId = renamePkSide ? pkTableId : fkTableId;
    String newTableName = renamePkSide ? "accounts" : "orders_v2";
    var renameResult = changeTableNameUseCase.changeTableName(new ChangeTableNameCommand(
        targetTableId,
        newTableName)).block();

    assertThat(renameResult.affectedTableIds())
        .containsExactlyInAnyOrder(pkTableId, fkTableId);

    String renameOpId = renameResult.operation().opId();

    var undoResult = undoErdOperationUseCase.undo(new UndoErdOperationCommand(renameOpId)).block();
    assertThat(undoResult.affectedTableIds())
        .containsExactlyInAnyOrder(pkTableId, fkTableId);

    var redoResult = redoErdOperationUseCase.redo(new RedoErdOperationCommand(renameOpId)).block();
    assertThat(redoResult.affectedTableIds())
        .containsExactlyInAnyOrder(pkTableId, fkTableId);
    assertLastOperation(schemaId, "CHANGE_TABLE_NAME", currentRevision(schemaId), List.of(pkTableId, fkTableId));
  }

  @Test
  @DisplayName("index column 제거 undo/redo는 parent index와 membership row를 원래 ID로 복원한다")
  void indexColumnRemovalUndoRedoRestoresParentIndexAndColumnMembership() {
    String projectId = createActiveProjectId("erd_operation_index_column_undo_redo");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "index_column_undo_redo_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    String columnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        tableId,
        "email",
        "VARCHAR",
        255,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().columnId();

    String indexId = createIndexUseCase.createIndex(new CreateIndexCommand(
        tableId,
        "idx_users_email",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(columnId, 0, SortDirection.ASC)))).block().result().indexId();
    String indexColumnId = firstIndexColumnId(indexId);

    var removeResult = removeIndexColumnUseCase.removeIndexColumn(new RemoveIndexColumnCommand(indexColumnId)).block();
    String removeOpId = removeResult.operation().opId();

    assertThat(rowExists("db_index_columns", indexColumnId)).isFalse();
    assertThat(rowExists("db_indexes", indexId)).isFalse();

    undoErdOperationUseCase.undo(new UndoErdOperationCommand(removeOpId)).block();
    assertThat(rowExists("db_indexes", indexId)).isTrue();
    assertThat(rowExists("db_index_columns", indexColumnId)).isTrue();
    assertThat(indexColumnSeqNo(indexColumnId)).isEqualTo(0);

    redoErdOperationUseCase.redo(new RedoErdOperationCommand(removeOpId)).block();
    assertThat(rowExists("db_index_columns", indexColumnId)).isFalse();
    assertThat(rowExists("db_indexes", indexId)).isFalse();
    assertLastOperation(schemaId, "REMOVE_INDEX_COLUMN", currentRevision(schemaId), List.of(tableId));
  }

  @Test
  @DisplayName("relationship column 제거 undo/redo는 FK column과 orphan relationship을 원래 ID로 복원한다")
  void relationshipColumnRemovalUndoRedoRestoresFkColumnAndOrphanRelationship() {
    String projectId = createActiveProjectId("erd_operation_relationship_column_undo_redo");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "relationship_column_undo_redo_schema",
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
        "pk column")).block().result().columnId();

    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

    String relationshipId = createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null)).block().result().relationshipId();
    String relationshipColumnId = firstRelationshipColumnId(relationshipId);
    String fkColumnId = relationshipColumnFkColumnId(relationshipColumnId);

    var removeResult = removeRelationshipColumnUseCase
        .removeRelationshipColumn(new RemoveRelationshipColumnCommand(relationshipColumnId))
        .block();
    String removeOpId = removeResult.operation().opId();

    assertThat(rowExists("db_relationship_columns", relationshipColumnId)).isFalse();
    assertThat(rowExists("db_relationships", relationshipId)).isFalse();
    assertThat(rowExists("db_columns", fkColumnId)).isFalse();

    undoErdOperationUseCase.undo(new UndoErdOperationCommand(removeOpId)).block();
    assertThat(rowExists("db_relationships", relationshipId)).isTrue();
    assertThat(rowExists("db_relationship_columns", relationshipColumnId)).isTrue();
    assertThat(rowExists("db_columns", fkColumnId)).isTrue();

    redoErdOperationUseCase.redo(new RedoErdOperationCommand(removeOpId)).block();
    assertThat(rowExists("db_relationship_columns", relationshipColumnId)).isFalse();
    assertThat(rowExists("db_relationships", relationshipId)).isFalse();
    assertThat(rowExists("db_columns", fkColumnId)).isFalse();
    assertLastOperation(schemaId, "REMOVE_RELATIONSHIP_COLUMN", currentRevision(schemaId), List.of(pkTableId, fkTableId));
  }

  @Test
  @DisplayName("structural snapshot reconcile은 같은 ID row 속성을 target snapshot과 동일하게 복원한다")
  void structuralSnapshotReconcileRestoresExistingRowAttributes() {
    String projectId = createActiveProjectId("erd_operation_structural_snapshot_restore");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "structural_snapshot_restore_schema",
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
        "VARCHAR",
        26,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        "pk column")).block().result().columnId();

    String otherPkColumnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        pkTableId,
        "legacy_id",
        "VARCHAR",
        26,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().columnId();

    String otherFkColumnId = createColumnUseCase.createColumn(new CreateColumnCommand(
        fkTableId,
        "legacy_user_id",
        "VARCHAR",
        26,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().columnId();

    String constraintId = createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkTableId,
        "pk_users",
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block().result().constraintId();
    String constraintColumnId = firstConstraintColumnId(constraintId);

    String indexId = createIndexUseCase.createIndex(new CreateIndexCommand(
        pkTableId,
        "idx_users_id",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(pkColumnId, 0, SortDirection.ASC)))).block().result().indexId();
    String indexColumnId = firstIndexColumnId(indexId);

    String relationshipId = createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkTableId,
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "{\"label\":\"target\"}")).block().result().relationshipId();
    String relationshipColumnId = firstRelationshipColumnId(relationshipId);

    StructuralSnapshot targetSnapshot = structuralSnapshotService.captureBySchemaId(schemaId).block();

    mutateStructuralSnapshotRows(
        pkTableId,
        fkTableId,
        pkColumnId,
        otherPkColumnId,
        otherFkColumnId,
        constraintId,
        constraintColumnId,
        indexId,
        indexColumnId,
        relationshipId,
        relationshipColumnId);

    assertThat(structuralSnapshotService.captureBySchemaId(schemaId).block())
        .isNotEqualTo(targetSnapshot);

    structuralSnapshotService.reconcileTo(targetSnapshot).block();

    assertThat(structuralSnapshotService.captureBySchemaId(schemaId).block())
        .isEqualTo(targetSnapshot);
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
  @DisplayName("파생 mutation은 resolve 당시 revision이 더 이상 최신이 아니면 커밋하지 않는다")
  void rejectsDerivedMutationWhenBaseRevisionIsStale() {
    String projectId = createActiveProjectId("erd_operation_stale_derived");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "stale_derived_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "stale_derived_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    long currentRevision = currentRevision(schemaId);
    long beforeCount = operationCount(schemaId);

    StepVerifier.create(changeTableExtraUseCase.changeTableExtra(new ChangeTableExtraCommand(
        tableId,
        "{\"comment\":\"stale\"}"))
        .contextWrite(ErdOperationContexts.withDerivation(ErdOperationDerivationKind.UNDO, "stale-parent")
            .andThen(ErdOperationContexts.withBaseSchemaRevision(currentRevision - 1))))
        .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.SUPERSEDED))
        .verify();

    assertThat(currentRevision(schemaId)).isEqualTo(currentRevision);
    assertThat(operationCount(schemaId)).isEqualTo(beforeCount);
    assertThat(tableExtraIsNull(tableId)).isTrue();
  }

  @Test
  @DisplayName("같은 operation을 부모로 하는 파생 operation log는 중복 기록할 수 없다")
  void enforcesUniqueDerivedFromOperationId() {
    String projectId = createActiveProjectId("erd_operation_unique_derived_from");

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "unique_derived_from_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "unique_derived_from_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();

    long committedRevision = currentRevision(schemaId) + 10;
    String parentOpId = latestOperationId(schemaId);

    StepVerifier.create(insertDerivedOperationLog(
        UlidGenerator.generate(),
        projectId,
        schemaId,
        committedRevision,
        parentOpId))
        .expectNext(1L)
        .verifyComplete();

    StepVerifier.create(insertDerivedOperationLog(
        UlidGenerator.generate(),
        projectId,
        schemaId,
        committedRevision + 1,
        parentOpId))
        .expectError(DataIntegrityViolationException.class)
        .verify();
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

  private boolean tableExtraIsNull(String tableId) {
    return databaseClient.sql("""
        SELECT COUNT(*) AS cnt
        FROM db_tables
        WHERE id = :tableId
          AND extra IS NULL
        """)
        .bind("tableId", tableId)
        .map((row, metadata) -> ((Number) row.get("cnt")).longValue() == 1)
        .one()
        .block();
  }

  private String latestOperationId(String schemaId) {
    return databaseClient.sql("""
        SELECT op_id
        FROM erd_operation_log
        WHERE schema_id = :schemaId
        ORDER BY committed_revision DESC
        LIMIT 1
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> row.get("op_id", String.class))
        .one()
        .block();
  }

  private Mono<Long> insertDerivedOperationLog(
      String opId,
      String projectId,
      String schemaId,
      long committedRevision,
      String derivedFromOpId) {
    return databaseClient.sql("""
        INSERT INTO erd_operation_log (
            op_id,
            project_id,
            schema_id,
            op_type,
            committed_revision,
            base_schema_revision,
            client_operation_id,
            collab_session_id,
            actor_user_id,
            derivation_kind,
            derived_from_op_id,
            lifecycle_state,
            payload_json,
            inverse_payload_json,
            touched_entities_json,
            affected_table_ids_json
        ) VALUES (
            :opId,
            :projectId,
            :schemaId,
            'CHANGE_TABLE_EXTRA',
            :committedRevision,
            :baseSchemaRevision,
            NULL,
            NULL,
            'system',
            'UNDO',
            :derivedFromOpId,
            'COMMITTED',
            '{}',
            NULL,
            '[]',
            '[]'
        )
        """)
        .bind("opId", opId)
        .bind("projectId", projectId)
        .bind("schemaId", schemaId)
        .bind("committedRevision", committedRevision)
        .bind("baseSchemaRevision", committedRevision - 1)
        .bind("derivedFromOpId", derivedFromOpId)
        .fetch()
        .rowsUpdated();
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

  private boolean rowExists(String tableName, String id) {
    return databaseClient.sql("""
        SELECT COUNT(*) AS cnt
        FROM %s
        WHERE id = :id
        """.formatted(tableName))
        .bind("id", id)
        .map((row, metadata) -> ((Number) row.get("cnt")).longValue() == 1)
        .one()
        .block();
  }

  private void mutateStructuralSnapshotRows(
      String pkTableId,
      String fkTableId,
      String pkColumnId,
      String otherPkColumnId,
      String otherFkColumnId,
      String constraintId,
      String constraintColumnId,
      String indexId,
      String indexColumnId,
      String relationshipId,
      String relationshipColumnId) {
    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_columns
        SET table_id = :fkTableId,
            name = 'id_mutated',
            data_type = 'INT',
            type_arguments = NULL,
            seq_no = 7,
            auto_increment = TRUE,
            charset = 'latin1',
            collation = 'latin1_swedish_ci',
            comment = 'mutated column'
        WHERE id = :pkColumnId
        """)
        .bind("fkTableId", fkTableId)
        .bind("pkColumnId", pkColumnId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_constraints
        SET table_id = :fkTableId,
            name = 'pk_mutated',
            kind = 'UNIQUE',
            check_expr = 'mutated_check',
            default_expr = 'mutated_default'
        WHERE id = :constraintId
        """)
        .bind("fkTableId", fkTableId)
        .bind("constraintId", constraintId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_constraint_columns
        SET column_id = :otherPkColumnId,
            seq_no = 3
        WHERE id = :constraintColumnId
        """)
        .bind("otherPkColumnId", otherPkColumnId)
        .bind("constraintColumnId", constraintColumnId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_indexes
        SET table_id = :fkTableId,
            name = 'idx_mutated',
            type = 'HASH'
        WHERE id = :indexId
        """)
        .bind("fkTableId", fkTableId)
        .bind("indexId", indexId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_index_columns
        SET column_id = :otherPkColumnId,
            seq_no = 4,
            sort_dir = 'DESC'
        WHERE id = :indexColumnId
        """)
        .bind("otherPkColumnId", otherPkColumnId)
        .bind("indexColumnId", indexColumnId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_relationships
        SET pk_table_id = :fkTableId,
            fk_table_id = :pkTableId,
            name = 'rel_mutated',
            kind = 'IDENTIFYING',
            cardinality = 'ONE_TO_ONE',
            extra = '{"label":"mutated"}'
        WHERE id = :relationshipId
        """)
        .bind("fkTableId", fkTableId)
        .bind("pkTableId", pkTableId)
        .bind("relationshipId", relationshipId)
        .fetch()
        .rowsUpdated());

    assertSingleRowUpdated(databaseClient.sql("""
        UPDATE db_relationship_columns
        SET pk_column_id = :otherPkColumnId,
            fk_column_id = :otherFkColumnId,
            seq_no = 5
        WHERE id = :relationshipColumnId
        """)
        .bind("otherPkColumnId", otherPkColumnId)
        .bind("otherFkColumnId", otherFkColumnId)
        .bind("relationshipColumnId", relationshipColumnId)
        .fetch()
        .rowsUpdated());
  }

  private void assertSingleRowUpdated(Mono<Long> rowsUpdated) {
    assertThat(rowsUpdated.block()).isEqualTo(1L);
  }

  private String firstConstraintColumnId(String constraintId) {
    return databaseClient.sql("""
        SELECT id
        FROM db_constraint_columns
        WHERE constraint_id = :constraintId
        ORDER BY seq_no
        LIMIT 1
        """)
        .bind("constraintId", constraintId)
        .map((row, metadata) -> row.get("id", String.class))
        .one()
        .block();
  }

  private String firstIndexColumnId(String indexId) {
    return databaseClient.sql("""
        SELECT id
        FROM db_index_columns
        WHERE index_id = :indexId
        ORDER BY seq_no
        LIMIT 1
        """)
        .bind("indexId", indexId)
        .map((row, metadata) -> row.get("id", String.class))
        .one()
        .block();
  }

  private int indexColumnSeqNo(String indexColumnId) {
    return databaseClient.sql("""
        SELECT seq_no
        FROM db_index_columns
        WHERE id = :indexColumnId
        """)
        .bind("indexColumnId", indexColumnId)
        .map((row, metadata) -> ((Number) row.get("seq_no")).intValue())
        .one()
        .block();
  }

  private String firstRelationshipColumnId(String relationshipId) {
    return databaseClient.sql("""
        SELECT id
        FROM db_relationship_columns
        WHERE relationship_id = :relationshipId
        ORDER BY seq_no
        LIMIT 1
        """)
        .bind("relationshipId", relationshipId)
        .map((row, metadata) -> row.get("id", String.class))
        .one()
        .block();
  }

  private String relationshipColumnFkColumnId(String relationshipColumnId) {
    return databaseClient.sql("""
        SELECT fk_column_id
        FROM db_relationship_columns
        WHERE id = :relationshipColumnId
        """)
        .bind("relationshipColumnId", relationshipColumnId)
        .map((row, metadata) -> row.get("fk_column_id", String.class))
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
    return StreamSupport.stream(node.spliterator(), false)
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

}
