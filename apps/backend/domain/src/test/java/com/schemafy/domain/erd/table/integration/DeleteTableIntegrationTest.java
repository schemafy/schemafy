package com.schemafy.domain.erd.table.integration;

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
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.domain.erd.column.application.port.in.GetColumnsByTableIdUseCase;
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
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.in.GetTableQuery;
import com.schemafy.domain.erd.table.application.port.in.GetTableUseCase;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Table Cascade 삭제 통합 테스트")
class DeleteTableIntegrationTest {

  @Autowired
  CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  CreateTableUseCase createTableUseCase;

  @Autowired
  DeleteTableUseCase deleteTableUseCase;

  @Autowired
  GetTableUseCase getTableUseCase;

  @Autowired
  CreateColumnUseCase createColumnUseCase;

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
  @DisplayName("deleteTable 메서드는")
  class DeleteTable {

    @Nested
    @DisplayName("테이블에 관련 엔티티(컬럼, 제약조건, 인덱스, 릴레이션)가 있을 때")
    class WithAllRelatedEntities {

      private String schemaId;
      private String fkTableId;
      private String pkTableId;
      private String fkColumnId;
      private String pkColumnId;

      @BeforeEach
      void setUp() {
        var createSchemaCommand = new CreateSchemaCommand(
            PROJECT_ID, "MySQL", "table_cascade_delete_test_schema",
            "utf8mb4", "utf8mb4_general_ci");
        var schemaResult = createSchemaUseCase.createSchema(createSchemaCommand).block();
        schemaId = schemaResult.id();

        var createPkTableCommand = new CreateTableCommand(
            schemaId, "pk_table", "utf8mb4", "utf8mb4_general_ci");
        var pkTableResult = createTableUseCase.createTable(createPkTableCommand).block();
        pkTableId = pkTableResult.tableId();

        var createFkTableCommand = new CreateTableCommand(
            schemaId, "fk_table", "utf8mb4", "utf8mb4_general_ci");
        var fkTableResult = createTableUseCase.createTable(createFkTableCommand).block();
        fkTableId = fkTableResult.tableId();

        var createPkColumnCommand = new CreateColumnCommand(
            pkTableId, "id", "INT", null, null, null, 0, true, null, null, "PK column");
        var pkColumnResult = createColumnUseCase.createColumn(createPkColumnCommand).block();
        pkColumnId = pkColumnResult.columnId();

        var createFkColumnCommand = new CreateColumnCommand(
            fkTableId, "pk_id", "INT", null, null, null, 0, false, null, null, "FK column");
        var fkColumnResult = createColumnUseCase.createColumn(createFkColumnCommand).block();
        fkColumnId = fkColumnResult.columnId();

        var createConstraintCommand = new CreateConstraintCommand(
            fkTableId, "fk_not_null", ConstraintKind.NOT_NULL, null, null,
            List.of(new CreateConstraintColumnCommand(fkColumnId, 0)));
        createConstraintUseCase.createConstraint(createConstraintCommand).block();

        var createIndexCommand = new CreateIndexCommand(
            fkTableId, "idx_pk_id", IndexType.BTREE,
            List.of(new CreateIndexColumnCommand(fkColumnId, 0, SortDirection.ASC)));
        createIndexUseCase.createIndex(createIndexCommand).block();

        var createRelationshipCommand = new CreateRelationshipCommand(
            fkTableId, pkTableId, "fk_relationship",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null,
            List.of(new CreateRelationshipColumnCommand(pkColumnId, fkColumnId, 0)));
        createRelationshipUseCase.createRelationship(createRelationshipCommand).block();
      }

      @Test
      @DisplayName("테이블과 관련된 컬럼, 제약조건, 인덱스, 릴레이션을 모두 삭제한다")
      void deletesTableAndAllRelatedEntities() {
        StepVerifier.create(deleteTableUseCase.deleteTable(
            new DeleteTableCommand(fkTableId)))
            .verifyComplete();

        StepVerifier.create(getTableUseCase.getTable(
            new GetTableQuery(fkTableId)))
            .expectError(TableNotExistException.class)
            .verify();

        StepVerifier.create(getColumnsByTableIdUseCase.getColumnsByTableId(
            new GetColumnsByTableIdQuery(fkTableId)))
            .assertNext(columns -> assertThat(columns).isEmpty())
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
      }

    }

  }

}
