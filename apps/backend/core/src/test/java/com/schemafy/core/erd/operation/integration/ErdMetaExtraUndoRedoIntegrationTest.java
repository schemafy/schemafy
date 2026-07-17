package com.schemafy.core.erd.operation.integration;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.PatchField;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaUseCase;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.core.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraUseCase;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.support.ErdProjectIntegrationSupport;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ERD meta/extra Undo/Redo 통합 테스트")
class ErdMetaExtraUndoRedoIntegrationTest extends ErdProjectIntegrationSupport {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  ChangeTableMetaUseCase changeTableMetaUseCase;

  @Autowired
  ChangeTableExtraUseCase changeTableExtraUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  ChangeColumnMetaUseCase changeColumnMetaUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  ChangeConstraintCheckExprUseCase changeConstraintCheckExprUseCase;

  @Autowired
  ChangeConstraintDefaultExprUseCase changeConstraintDefaultExprUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  ChangeIndexColumnSortDirectionUseCase changeIndexColumnSortDirectionUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  ChangeRelationshipExtraUseCase changeRelationshipExtraUseCase;

  @Autowired
  UndoErdOperationUseCase undoErdOperationUseCase;

  @Autowired
  RedoErdOperationUseCase redoErdOperationUseCase;

  @Autowired
  GetTableByIdPort getTableByIdPort;

  @Autowired
  GetColumnByIdPort getColumnByIdPort;

  @Autowired
  GetConstraintByIdPort getConstraintByIdPort;

  @Autowired
  GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Autowired
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Autowired
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Autowired
  JsonCodec jsonCodec;

  @Test
  @DisplayName("table meta의 부분 nullable 변경을 undo/redo한다")
  void tableMetaRoundTripPreservesPartialNullableChange() {
    SchemaTableFixture fixture = createSchemaTableFixture("table_meta_round_trip", null);

    MutationResult<Void> original = changeTableMetaUseCase.changeTableMeta(new ChangeTableMetaCommand(
        fixture.tableId(),
        PatchField.of(null),
        PatchField.absent())).block();

    assertThat(getTable(fixture.tableId()).charset()).isNull();
    assertThat(getTable(fixture.tableId()).collation()).isEqualTo("utf8mb4_general_ci");

    MutationResult<Void> undo = undo(original);
    assertThat(getTable(fixture.tableId()).charset()).isEqualTo("utf8mb4");
    assertThat(getTable(fixture.tableId()).collation()).isEqualTo("utf8mb4_general_ci");

    MutationResult<Void> redo = redo(original);
    assertThat(getTable(fixture.tableId()).charset()).isNull();
    assertThat(getTable(fixture.tableId()).collation()).isEqualTo("utf8mb4_general_ci");
    assertRoundTripMetadata(original, undo, redo, Set.of(fixture.tableId()));
  }

  @Test
  @DisplayName("table position extra 전체 JSON을 undo/redo하고 no-op과 SUPERSEDED 정책을 유지한다")
  void tableExtraRoundTripPreservesWholeJsonAndEligibilityPolicies() {
    JsonNode oldExtra = json("{\"position\":{\"x\":10,\"y\":20},\"locked\":true}");
    JsonNode newExtra = json("{\"position\":{\"x\":100,\"y\":200},\"locked\":true}");
    SchemaTableFixture fixture = createSchemaTableFixture("table_extra_round_trip", oldExtra);

    long revisionBeforeNoOp = currentRevision(fixture.schemaId());
    MutationResult<Void> noOp = changeTableExtraUseCase.changeTableExtra(
        new ChangeTableExtraCommand(fixture.tableId(), oldExtra)).block();

    assertThat(noOp.operation()).isNull();
    assertThat(currentRevision(fixture.schemaId())).isEqualTo(revisionBeforeNoOp);

    MutationResult<Void> original = changeTableExtraUseCase.changeTableExtra(
        new ChangeTableExtraCommand(fixture.tableId(), newExtra)).block();
    assertJsonEquals(getTable(fixture.tableId()).extra(), newExtra);

    MutationResult<Void> undo = undo(original);
    assertJsonEquals(getTable(fixture.tableId()).extra(), oldExtra);

    MutationResult<Void> redo = redo(original);
    assertJsonEquals(getTable(fixture.tableId()).extra(), newExtra);
    assertRoundTripMetadata(original, undo, redo, Set.of(fixture.tableId()));

    changeTableMetaUseCase.changeTableMeta(new ChangeTableMetaCommand(
        fixture.tableId(),
        PatchField.of("latin1"),
        PatchField.of("latin1_swedish_ci"))).block();

    StepVerifier.create(undoErdOperationUseCase.undo(new UndoErdOperationCommand(original.operation().opId())))
        .expectErrorMatches(DomainException.hasErrorCode(OperationErrorCode.SUPERSEDED))
        .verify();
  }

  @Test
  @DisplayName("column meta 부분 변경과 FK cascade를 undo/redo한다")
  void columnMetaRoundTripRestoresFkCascade() {
    RelationshipFixture fixture = createRelationshipFixture("column_meta_round_trip", null);
    RelationshipColumn relationshipColumn = relationshipColumns(fixture.relationshipId()).getFirst();
    String fkColumnId = relationshipColumn.fkColumnId();
    Column oldPkColumn = getColumn(fixture.pkColumnId());
    Column oldFkColumn = getColumn(fkColumnId);

    MutationResult<Void> original = changeColumnMetaUseCase.changeColumnMeta(new ChangeColumnMetaCommand(
        fixture.pkColumnId(),
        PatchField.absent(),
        PatchField.of("latin1"),
        PatchField.of("latin1_swedish_ci"),
        PatchField.of("updated pk comment"))).block();

    assertColumnMeta(fixture.pkColumnId(), "latin1", "latin1_swedish_ci", "updated pk comment");
    assertColumnMeta(fkColumnId, "latin1", "latin1_swedish_ci", oldFkColumn.comment());

    MutationResult<Void> undo = undo(original);
    assertColumnMeta(
        fixture.pkColumnId(), oldPkColumn.charset(), oldPkColumn.collation(), oldPkColumn.comment());
    assertColumnMeta(fkColumnId, oldFkColumn.charset(), oldFkColumn.collation(), oldFkColumn.comment());

    MutationResult<Void> redo = redo(original);
    assertColumnMeta(fixture.pkColumnId(), "latin1", "latin1_swedish_ci", "updated pk comment");
    assertColumnMeta(fkColumnId, "latin1", "latin1_swedish_ci", oldFkColumn.comment());
    assertRoundTripMetadata(original, undo, redo, Set.of(fixture.pkTableId(), fixture.fkTableId()));
  }

  @Test
  @DisplayName("relationship control point extra 전체 JSON을 undo/redo한다")
  void relationshipExtraRoundTripPreservesWholeJson() {
    JsonNode oldExtra = json("{\"controlPoint1\":{\"x\":10,\"y\":20},\"fkHandle\":\"left\"}");
    JsonNode newExtra = json("{\"controlPoint1\":{\"x\":100,\"y\":200},\"controlPoint2\":{\"x\":300,\"y\":400}}");
    RelationshipFixture fixture = createRelationshipFixture("relationship_extra_round_trip", oldExtra);

    MutationResult<Void> original = changeRelationshipExtraUseCase.changeRelationshipExtra(
        new ChangeRelationshipExtraCommand(fixture.relationshipId(), newExtra)).block();
    assertJsonEquals(getRelationship(fixture.relationshipId()).extra(), newExtra);

    MutationResult<Void> undo = undo(original);
    assertJsonEquals(getRelationship(fixture.relationshipId()).extra(), oldExtra);

    MutationResult<Void> redo = redo(original);
    assertJsonEquals(getRelationship(fixture.relationshipId()).extra(), newExtra);
    assertRoundTripMetadata(original, undo, redo, Set.of(fixture.pkTableId(), fixture.fkTableId()));
  }

  @Test
  @DisplayName("CHECK와 DEFAULT expression의 nullable 값을 undo/redo한다")
  void constraintExpressionRoundTripPreservesNullableValues() {
    SchemaTableFixture fixture = createSchemaTableFixture("constraint_expression_round_trip", null);
    String checkColumnId = createColumn(fixture.tableId(), "check_value");
    String defaultColumnId = createColumn(fixture.tableId(), "default_value");
    String checkConstraintId = createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        fixture.tableId(),
        "ck_value",
        ConstraintKind.CHECK,
        "check_value > 0",
        null,
        List.of(new CreateConstraintColumnCommand(checkColumnId, 0)))).block().result().constraintId();
    String defaultConstraintId = createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        fixture.tableId(),
        "df_value",
        ConstraintKind.DEFAULT,
        null,
        "0",
        List.of(new CreateConstraintColumnCommand(defaultColumnId, 0)))).block().result().constraintId();

    MutationResult<Void> checkOriginal = changeConstraintCheckExprUseCase.changeConstraintCheckExpr(
        new ChangeConstraintCheckExprCommand(checkConstraintId, null)).block();
    assertThat(getConstraintByIdPort.findConstraintById(checkConstraintId).block().checkExpr()).isNull();
    MutationResult<Void> checkUndo = undo(checkOriginal);
    assertThat(getConstraintByIdPort.findConstraintById(checkConstraintId).block().checkExpr())
        .isEqualTo("check_value > 0");
    MutationResult<Void> checkRedo = redo(checkOriginal);
    assertThat(getConstraintByIdPort.findConstraintById(checkConstraintId).block().checkExpr()).isNull();
    assertRoundTripMetadata(checkOriginal, checkUndo, checkRedo, Set.of(fixture.tableId()));

    MutationResult<Void> defaultOriginal = changeConstraintDefaultExprUseCase.changeConstraintDefaultExpr(
        new ChangeConstraintDefaultExprCommand(defaultConstraintId, "1")).block();
    assertThat(getConstraintByIdPort.findConstraintById(defaultConstraintId).block().defaultExpr())
        .isEqualTo("1");
    MutationResult<Void> defaultUndo = undo(defaultOriginal);
    assertThat(getConstraintByIdPort.findConstraintById(defaultConstraintId).block().defaultExpr())
        .isEqualTo("0");
    MutationResult<Void> defaultRedo = redo(defaultOriginal);
    assertThat(getConstraintByIdPort.findConstraintById(defaultConstraintId).block().defaultExpr())
        .isEqualTo("1");
    assertRoundTripMetadata(defaultOriginal, defaultUndo, defaultRedo, Set.of(fixture.tableId()));
  }

  @Test
  @DisplayName("index column sort direction을 undo/redo한다")
  void indexColumnSortDirectionRoundTrip() {
    SchemaTableFixture fixture = createSchemaTableFixture("index_sort_round_trip", null);
    String columnId = createColumn(fixture.tableId(), "indexed_value");
    String indexId = createIndexUseCase.createIndex(new CreateIndexCommand(
        fixture.tableId(),
        "idx_value",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(columnId, 0, SortDirection.ASC)))).block().result().indexId();
    String indexColumnId = databaseClient.sql("""
        SELECT id
        FROM db_index_columns
        WHERE index_id = :indexId
        """)
        .bind("indexId", indexId)
        .map((row, metadata) -> row.get("id", String.class))
        .one()
        .block();

    MutationResult<Void> original = changeIndexColumnSortDirectionUseCase.changeIndexColumnSortDirection(
        new ChangeIndexColumnSortDirectionCommand(indexColumnId, SortDirection.DESC)).block();
    assertThat(getIndexColumnByIdPort.findIndexColumnById(indexColumnId).block().sortDirection())
        .isEqualTo(SortDirection.DESC);

    MutationResult<Void> undo = undo(original);
    assertThat(getIndexColumnByIdPort.findIndexColumnById(indexColumnId).block().sortDirection())
        .isEqualTo(SortDirection.ASC);

    MutationResult<Void> redo = redo(original);
    assertThat(getIndexColumnByIdPort.findIndexColumnById(indexColumnId).block().sortDirection())
        .isEqualTo(SortDirection.DESC);
    assertRoundTripMetadata(original, undo, redo, Set.of(fixture.tableId()));
  }

  private SchemaTableFixture createSchemaTableFixture(String prefix, JsonNode tableExtra) {
    String projectId = createActiveProjectId(prefix);
    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        prefix + "_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();
    String tableId = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        prefix + "_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        tableExtra)).block().result().tableId();
    return new SchemaTableFixture(schemaId, tableId);
  }

  private RelationshipFixture createRelationshipFixture(String prefix, JsonNode relationshipExtra) {
    SchemaTableFixture pkFixture = createSchemaTableFixture(prefix, null);
    String fkTableId = createTableUseCase.createTable(new CreateTableCommand(
        pkFixture.schemaId(),
        prefix + "_fk_table",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result().tableId();
    String pkColumnId = createColumn(pkFixture.tableId(), "id");
    createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        pkFixture.tableId(),
        "pk_" + prefix,
        ConstraintKind.PRIMARY_KEY,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();
    String relationshipId = createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
        fkTableId,
        pkFixture.tableId(),
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        relationshipExtra)).block().result().relationshipId();
    return new RelationshipFixture(
        pkFixture.schemaId(),
        pkFixture.tableId(),
        fkTableId,
        pkColumnId,
        relationshipId);
  }

  private String createColumn(String tableId, String name) {
    return createColumnUseCase.createColumn(new CreateColumnCommand(
        tableId,
        name,
        "VARCHAR",
        255,
        null,
        null,
        false,
        "utf8mb4",
        "utf8mb4_general_ci",
        "initial comment")).block().result().columnId();
  }

  private MutationResult<Void> undo(MutationResult<Void> original) {
    return undoErdOperationUseCase.undo(new UndoErdOperationCommand(original.operation().opId())).block();
  }

  private MutationResult<Void> redo(MutationResult<Void> original) {
    return redoErdOperationUseCase.redo(new RedoErdOperationCommand(original.operation().opId())).block();
  }

  private void assertRoundTripMetadata(
      MutationResult<Void> original,
      MutationResult<Void> undo,
      MutationResult<Void> redo,
      Set<String> expectedAffectedTableIds) {
    CommittedErdOperation originalOperation = original.operation();
    CommittedErdOperation undoOperation = undo.operation();
    CommittedErdOperation redoOperation = redo.operation();

    assertThat(originalOperation.derivationKind()).isEqualTo(ErdOperationDerivationKind.ORIGINAL);
    assertThat(undoOperation.derivationKind()).isEqualTo(ErdOperationDerivationKind.UNDO);
    assertThat(redoOperation.derivationKind()).isEqualTo(ErdOperationDerivationKind.REDO);
    assertThat(undoOperation.committedRevision()).isEqualTo(originalOperation.committedRevision() + 1);
    assertThat(redoOperation.committedRevision()).isEqualTo(undoOperation.committedRevision() + 1);
    assertThat(derivedFromOperationId(undoOperation.opId())).isEqualTo(originalOperation.opId());
    assertThat(derivedFromOperationId(redoOperation.opId())).isEqualTo(undoOperation.opId());
    assertThat(original.affectedTableIds()).containsExactlyInAnyOrderElementsOf(expectedAffectedTableIds);
    assertThat(undo.affectedTableIds()).containsExactlyInAnyOrderElementsOf(expectedAffectedTableIds);
    assertThat(redo.affectedTableIds()).containsExactlyInAnyOrderElementsOf(expectedAffectedTableIds);
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

  private String derivedFromOperationId(String opId) {
    return databaseClient.sql("""
        SELECT derived_from_op_id
        FROM erd_operation_log
        WHERE op_id = :opId
        """)
        .bind("opId", opId)
        .map((row, metadata) -> row.get("derived_from_op_id", String.class))
        .one()
        .block();
  }

  private com.schemafy.core.erd.table.domain.Table getTable(String tableId) {
    return getTableByIdPort.findTableById(tableId).block();
  }

  private Column getColumn(String columnId) {
    return getColumnByIdPort.findColumnById(columnId).block();
  }

  private com.schemafy.core.erd.relationship.domain.Relationship getRelationship(String relationshipId) {
    return getRelationshipByIdPort.findRelationshipById(relationshipId).block();
  }

  private List<RelationshipColumn> relationshipColumns(String relationshipId) {
    return getRelationshipColumnsByRelationshipIdPort
        .findRelationshipColumnsByRelationshipId(relationshipId)
        .block();
  }

  private void assertColumnMeta(
      String columnId,
      String expectedCharset,
      String expectedCollation,
      String expectedComment) {
    Column column = getColumn(columnId);
    assertThat(column.charset()).isEqualTo(expectedCharset);
    assertThat(column.collation()).isEqualTo(expectedCollation);
    assertThat(column.comment()).isEqualTo(expectedComment);
  }

  private JsonNode json(String rawJson) {
    return jsonCodec.fromJson(rawJson, JsonNode.class);
  }

  private void assertJsonEquals(String actualJson, JsonNode expectedJson) {
    assertThat(jsonCodec.fromPersistedJson(actualJson, JsonNode.class)).isEqualTo(expectedJson);
  }

  private record SchemaTableFixture(String schemaId, String tableId) {

  }

  private record RelationshipFixture(
      String schemaId,
      String pkTableId,
      String fkTableId,
      String pkColumnId,
      String relationshipId) {

  }

}
