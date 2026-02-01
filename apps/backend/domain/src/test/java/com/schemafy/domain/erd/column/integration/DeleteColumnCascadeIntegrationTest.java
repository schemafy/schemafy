package com.schemafy.domain.erd.column.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.domain.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnUseCase;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdQuery;
import com.schemafy.domain.erd.constraint.application.port.in.GetConstraintsByTableIdUseCase;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdQuery;
import com.schemafy.domain.erd.index.application.port.in.GetIndexesByTableIdUseCase;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipColumnCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.domain.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdQuery;
import com.schemafy.domain.erd.relationship.application.port.in.GetRelationshipsByTableIdUseCase;
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
@DisplayName("Column Cascade 삭제 통합 테스트")
class DeleteColumnCascadeIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

  @Autowired
  DeleteColumnUseCase deleteColumnUseCase;

  @Autowired
  GetColumnUseCase getColumnUseCase;

  @Autowired
  GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @Autowired
  CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  GetConstraintsByTableIdUseCase getConstraintsByTableIdUseCase;

  @Autowired
  CreateIndexUseCase createIndexUseCase;

  @Autowired
  GetIndexesByTableIdUseCase getIndexesByTableIdUseCase;

  @Autowired
  CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  GetRelationshipsByTableIdUseCase getRelationshipsByTableIdUseCase;

  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69GPROJ";

  @Nested
  @DisplayName("PK 컬럼 삭제 시")
  class DeletePkColumn {

    @Nested
    @DisplayName("FK 컬럼이 하나 있을 때")
    class WithSingleFkColumn {

      private String schemaId;
      private String pkTableId;
      private String fkTableId;
      private String pkColumnId;
      private String fkColumnId;

      @BeforeEach
      void setUp() {
        var schemaResult = createSchemaUseCase.createSchema(new CreateSchemaCommand(
            PROJECT_ID, "MySQL", "pk_delete_single_fk_schema",
            "utf8mb4", "utf8mb4_general_ci")).block();
        schemaId = schemaResult.id();

        var pkTableResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci")).block();
        pkTableId = pkTableResult.tableId();

        var fkTableResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "fk_table", "utf8mb4", "utf8mb4_general_ci")).block();
        fkTableId = fkTableResult.tableId();

        var pkColumnResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            pkTableId, "id", "INT", null, null, null, 0, true, null, null, null)).block();
        pkColumnId = pkColumnResult.columnId();

        var fkColumnResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            fkTableId, "pk_id", "INT", null, null, null, 0, false, null, null, null)).block();
        fkColumnId = fkColumnResult.columnId();

        createConstraintUseCase.createConstraint(new CreateConstraintCommand(
            pkTableId, "pk_id_constraint", ConstraintKind.PRIMARY_KEY, null, null,
            List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

        createConstraintUseCase.createConstraint(new CreateConstraintCommand(
            fkTableId, "fk_not_null", ConstraintKind.NOT_NULL, null, null,
            List.of(new CreateConstraintColumnCommand(fkColumnId, 0)))).block();

        createIndexUseCase.createIndex(new CreateIndexCommand(
            fkTableId, "idx_fk_col", IndexType.BTREE,
            List.of(new CreateIndexColumnCommand(fkColumnId, 0, SortDirection.ASC)))).block();

        createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
            fkTableId, pkTableId, "fk_relationship",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)))).block();
      }

      @Test
      @DisplayName("FK 컬럼과 관련 Constraint, Index, Relationship이 모두 cascade 삭제된다")
      void cascadeDeletesFkColumnAndRelatedEntities() {
        StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(pkColumnId)))
            .verifyComplete();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(pkColumnId)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(fkColumnId)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getConstraintsByTableIdUseCase.getConstraintsByTableId(
            new GetConstraintsByTableIdQuery(fkTableId)))
            .assertNext(constraints -> assertThat(constraints).isEmpty())
            .verifyComplete();

        StepVerifier.create(getIndexesByTableIdUseCase.getIndexesByTableId(
            new GetIndexesByTableIdQuery(fkTableId)))
            .assertNext(indexes -> assertThat(indexes).isEmpty())
            .verifyComplete();

        StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
            new GetRelationshipsByTableIdQuery(fkTableId)))
            .assertNext(relationships -> assertThat(relationships).isEmpty())
            .verifyComplete();
      }

    }

    @Nested
    @DisplayName("다중 FK 테이블이 참조할 때")
    class WithMultipleFkTables {

      private String schemaId;
      private String pkTableId;
      private String fkTableId1;
      private String fkTableId2;
      private String pkColumnId;
      private String fkColumnId1;
      private String fkColumnId2;

      @BeforeEach
      void setUp() {
        var schemaResult = createSchemaUseCase.createSchema(new CreateSchemaCommand(
            PROJECT_ID, "MySQL", "pk_delete_multi_fk_schema",
            "utf8mb4", "utf8mb4_general_ci")).block();
        schemaId = schemaResult.id();

        var pkTableResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci")).block();
        pkTableId = pkTableResult.tableId();

        var fkTableResult1 = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "fk_table_1", "utf8mb4", "utf8mb4_general_ci")).block();
        fkTableId1 = fkTableResult1.tableId();

        var fkTableResult2 = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "fk_table_2", "utf8mb4", "utf8mb4_general_ci")).block();
        fkTableId2 = fkTableResult2.tableId();

        var pkColumnResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            pkTableId, "id", "INT", null, null, null, 0, true, null, null, null)).block();
        pkColumnId = pkColumnResult.columnId();

        var fkColumnResult1 = createColumnUseCase.createColumn(new CreateColumnCommand(
            fkTableId1, "ref_id", "INT", null, null, null, 0, false, null, null, null)).block();
        fkColumnId1 = fkColumnResult1.columnId();

        var fkColumnResult2 = createColumnUseCase.createColumn(new CreateColumnCommand(
            fkTableId2, "ref_id", "INT", null, null, null, 0, false, null, null, null)).block();
        fkColumnId2 = fkColumnResult2.columnId();

        createConstraintUseCase.createConstraint(new CreateConstraintCommand(
            pkTableId, "pk_constraint", ConstraintKind.PRIMARY_KEY, null, null,
            List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

        createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
            fkTableId1, pkTableId, "fk_rel_1",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId1, 0)))).block();

        createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
            fkTableId2, pkTableId, "fk_rel_2",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId2, 0)))).block();
      }

      @Test
      @DisplayName("모든 FK 컬럼과 Relationship이 cascade 삭제된다")
      void cascadeDeletesAllFkColumnsAndRelationships() {
        StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(pkColumnId)))
            .verifyComplete();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(fkColumnId1)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(fkColumnId2)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
            new GetRelationshipsByTableIdQuery(fkTableId1)))
            .assertNext(relationships -> assertThat(relationships).isEmpty())
            .verifyComplete();

        StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
            new GetRelationshipsByTableIdQuery(fkTableId2)))
            .assertNext(relationships -> assertThat(relationships).isEmpty())
            .verifyComplete();
      }

    }

    @Nested
    @DisplayName("체인 관계 A(PK) → B(FK/PK) → C(FK)일 때")
    class WithChainedRelationship {

      private String schemaId;
      private String tableAId;
      private String tableBId;
      private String tableCId;
      private String colAId;
      private String colBId;
      private String colCId;

      @BeforeEach
      void setUp() {
        var schemaResult = createSchemaUseCase.createSchema(new CreateSchemaCommand(
            PROJECT_ID, "MySQL", "chain_cascade_schema",
            "utf8mb4", "utf8mb4_general_ci")).block();
        schemaId = schemaResult.id();

        var tableAResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "table_a", "utf8mb4", "utf8mb4_general_ci")).block();
        tableAId = tableAResult.tableId();

        var tableBResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "table_b", "utf8mb4", "utf8mb4_general_ci")).block();
        tableBId = tableBResult.tableId();

        var tableCResult = createTableUseCase.createTable(new CreateTableCommand(
            schemaId, "table_c", "utf8mb4", "utf8mb4_general_ci")).block();
        tableCId = tableCResult.tableId();

        var colAResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            tableAId, "id", "INT", null, null, null, 0, true, null, null, null)).block();
        colAId = colAResult.columnId();

        var colBResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            tableBId, "a_id", "INT", null, null, null, 0, false, null, null, null)).block();
        colBId = colBResult.columnId();

        var colCResult = createColumnUseCase.createColumn(new CreateColumnCommand(
            tableCId, "b_id", "INT", null, null, null, 0, false, null, null, null)).block();
        colCId = colCResult.columnId();

        createConstraintUseCase.createConstraint(new CreateConstraintCommand(
            tableAId, "pk_a", ConstraintKind.PRIMARY_KEY, null, null,
            List.of(new CreateConstraintColumnCommand(colAId, 0)))).block();

        createConstraintUseCase.createConstraint(new CreateConstraintCommand(
            tableBId, "pk_b", ConstraintKind.PRIMARY_KEY, null, null,
            List.of(new CreateConstraintColumnCommand(colBId, 0)))).block();

        createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
            tableBId, tableAId, "fk_a_to_b",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(colAId, colBId, 0)))).block();

        createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
            tableCId, tableBId, "fk_b_to_c",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(colBId, colCId, 0)))).block();
      }

      @Test
      @DisplayName("A 컬럼 삭제 시 B, C 컬럼 모두 cascade 삭제된다")
      void cascadeDeletesEntireChain() {
        StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(colAId)))
            .verifyComplete();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(colAId)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(colBId)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(colCId)))
            .expectError(ColumnNotExistException.class)
            .verify();

        StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
            new GetRelationshipsByTableIdQuery(tableBId)))
            .assertNext(relationships -> assertThat(relationships).isEmpty())
            .verifyComplete();

        StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
            new GetRelationshipsByTableIdQuery(tableCId)))
            .assertNext(relationships -> assertThat(relationships).isEmpty())
            .verifyComplete();

        StepVerifier.create(getConstraintsByTableIdUseCase.getConstraintsByTableId(
            new GetConstraintsByTableIdQuery(tableBId)))
            .assertNext(constraints -> assertThat(constraints).isEmpty())
            .verifyComplete();
      }

    }

  }

  @Nested
  @DisplayName("FK 컬럼만 삭제할 때")
  class DeleteFkColumnOnly {

    private String schemaId;
    private String pkTableId;
    private String fkTableId;
    private String pkColumnId;
    private String fkColumnId;

    @BeforeEach
    void setUp() {
      var schemaResult = createSchemaUseCase.createSchema(new CreateSchemaCommand(
          PROJECT_ID, "MySQL", "fk_only_delete_schema",
          "utf8mb4", "utf8mb4_general_ci")).block();
      schemaId = schemaResult.id();

      var pkTableResult = createTableUseCase.createTable(new CreateTableCommand(
          schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci")).block();
      pkTableId = pkTableResult.tableId();

      var fkTableResult = createTableUseCase.createTable(new CreateTableCommand(
          schemaId, "fk_table", "utf8mb4", "utf8mb4_general_ci")).block();
      fkTableId = fkTableResult.tableId();

      var pkColumnResult = createColumnUseCase.createColumn(new CreateColumnCommand(
          pkTableId, "id", "INT", null, null, null, 0, true, null, null, null)).block();
      pkColumnId = pkColumnResult.columnId();

      var fkColumnResult = createColumnUseCase.createColumn(new CreateColumnCommand(
          fkTableId, "ref_id", "INT", null, null, null, 0, false, null, null, null)).block();
      fkColumnId = fkColumnResult.columnId();

      createConstraintUseCase.createConstraint(new CreateConstraintCommand(
          pkTableId, "pk_constraint", ConstraintKind.PRIMARY_KEY, null, null,
          List.of(new CreateConstraintColumnCommand(pkColumnId, 0)))).block();

      createConstraintUseCase.createConstraint(new CreateConstraintCommand(
          fkTableId, "fk_not_null", ConstraintKind.NOT_NULL, null, null,
          List.of(new CreateConstraintColumnCommand(fkColumnId, 0)))).block();

      createIndexUseCase.createIndex(new CreateIndexCommand(
          fkTableId, "idx_ref_id", IndexType.BTREE,
          List.of(new CreateIndexColumnCommand(fkColumnId, 0, SortDirection.ASC)))).block();

      createRelationshipUseCase.createRelationship(new CreateRelationshipCommand(
          fkTableId, pkTableId, "fk_relationship",
          RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
          List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)))).block();
    }

    @Test
    @DisplayName("FK 컬럼 삭제 시 관련 Constraint, Index, Relationship이 삭제되고 PK 컬럼은 유지된다")
    void deletesFkColumnWithoutAffectingPkColumn() {
      StepVerifier.create(deleteColumnUseCase.deleteColumn(new DeleteColumnCommand(fkColumnId)))
          .verifyComplete();

      StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(fkColumnId)))
          .expectError(ColumnNotExistException.class)
          .verify();

      StepVerifier.create(getColumnUseCase.getColumn(new GetColumnQuery(pkColumnId)))
          .assertNext(column -> assertThat(column.id()).isEqualTo(pkColumnId))
          .verifyComplete();

      StepVerifier.create(getConstraintsByTableIdUseCase.getConstraintsByTableId(
          new GetConstraintsByTableIdQuery(fkTableId)))
          .assertNext(constraints -> assertThat(constraints).isEmpty())
          .verifyComplete();

      StepVerifier.create(getIndexesByTableIdUseCase.getIndexesByTableId(
          new GetIndexesByTableIdQuery(fkTableId)))
          .assertNext(indexes -> assertThat(indexes).isEmpty())
          .verifyComplete();

      StepVerifier.create(getRelationshipsByTableIdUseCase.getRelationshipsByTableId(
          new GetRelationshipsByTableIdQuery(fkTableId)))
          .assertNext(relationships -> assertThat(relationships).isEmpty())
          .verifyComplete();

      StepVerifier.create(getConstraintsByTableIdUseCase.getConstraintsByTableId(
          new GetConstraintsByTableIdQuery(pkTableId)))
          .assertNext(constraints -> assertThat(constraints).hasSize(1))
          .verifyComplete();
    }

  }

}
