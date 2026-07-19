package com.schemafy.api.erd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnUseCase;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdQuery;
import com.schemafy.core.erd.column.application.port.in.GetColumnsByTableIdUseCase;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipUseCase;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;
import com.schemafy.core.project.application.access.ProjectAccessRequesterContext;
import com.schemafy.core.project.domain.ProjectRole;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("Schema export 통합 테스트")
class SchemaExportIntegrationTest extends ProjectHttpTestSupport {

  private static final String TEST_REQUESTER_ID = "06F9R1TKJ2PSMTF1CZG8A2M300";

  @Autowired
  private SchemaDdlExportOrchestrator schemaDdlExportOrchestrator;

  @Autowired
  private SchemaMermaidExportOrchestrator schemaMermaidExportOrchestrator;

  @Autowired
  private CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  private CreateTableUseCase createTableUseCase;

  @Autowired
  private CreateColumnUseCase createColumnUseCase;

  @Autowired
  private CreateConstraintUseCase createConstraintUseCase;

  @Autowired
  private CreateRelationshipUseCase createRelationshipUseCase;

  @Autowired
  private GetColumnsByTableIdUseCase getColumnsByTableIdUseCase;

  @Autowired
  private CreateIndexUseCase createIndexUseCase;

  @Test
  @DisplayName("저장된 ERD를 프로젝트 vendor 정책으로 DDL과 Mermaid에 내보낸다")
  void exportsPersistedErdAsDdlAndMermaid() {
    String projectId = saveProject(
        saveWorkspace("schema_export_workspace", "description").getId(),
        "schema_export_project",
        "description")
        .getId();
    addProjectMember(projectId, TEST_REQUESTER_ID, ProjectRole.ADMIN);

    String schemaId = blockAsRequester(createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "app_schema",
        "utf8mb4",
        "utf8mb4_general_ci"))).result().id();
    String usersTableId = createTable(schemaId, "users");
    String ordersTableId = createTable(schemaId, "orders");

    String userIdColumnId = createColumn(
        usersTableId, "user_id", "BIGINT", null, null, null, true, null);
    String emailColumnId = createColumn(
        usersTableId, "email", "VARCHAR", 255, null, null, false, null);
    String orderIdColumnId = createColumn(
        ordersTableId, "order_id", "BIGINT", null, null, null, true, null);
    createColumn(
        ordersTableId, "status", "ENUM", null, null, null, false,
        List.of("READY", "PAID"));
    createColumn(
        ordersTableId, "total", "DECIMAL", null, 10, 2, false, null);

    createConstraint(usersTableId, "pk_users", ConstraintKind.PRIMARY_KEY, userIdColumnId);
    createConstraint(usersTableId, "uk_users_email", ConstraintKind.UNIQUE, emailColumnId);
    createConstraint(ordersTableId, "pk_orders", ConstraintKind.PRIMARY_KEY, orderIdColumnId);

    var relationship = blockAsRequester(createRelationshipUseCase.createRelationship(
        new CreateRelationshipCommand(
            ordersTableId,
            usersTableId,
            RelationshipKind.NON_IDENTIFYING,
            Cardinality.ONE_TO_MANY,
            null))).result();

    String relationshipColumnId = blockAsRequester(getColumnsByTableIdUseCase
        .getColumnsByTableId(new GetColumnsByTableIdQuery(ordersTableId)))
        .stream()
        .filter(column -> column.name().equals("user_id"))
        .findFirst()
        .orElseThrow()
        .id();
    blockAsRequester(createIndexUseCase.createIndex(new CreateIndexCommand(
        ordersTableId,
        "idx_orders_user",
        IndexType.BTREE,
        List.of(new CreateIndexColumnCommand(relationshipColumnId, 0, SortDirection.ASC)))));

    var ddl = blockAsRequester(schemaDdlExportOrchestrator.exportSchemaDdl(schemaId, "mysql"));
    var mermaid = blockAsRequester(schemaMermaidExportOrchestrator.exportSchemaMermaid(schemaId));
    long currentRevision = currentRevision(schemaId);

    assertThat(ddl.schemaId()).isEqualTo(schemaId);
    assertThat(ddl.targetDbVendor()).isEqualTo("mysql");
    assertThat(ddl.currentRevision()).isEqualTo(currentRevision);
    assertThat(ddl.ddl()).contains(
        "-- Vendor: mysql",
        "CREATE TABLE `orders`",
        "CREATE TABLE `users`",
        "`email` VARCHAR(255)",
        "`status` ENUM('READY', 'PAID')",
        "`total` DECIMAL(10,2)",
        "PRIMARY KEY (`user_id`)",
        "PRIMARY KEY (`order_id`)",
        "ALTER TABLE `users` ADD UNIQUE KEY `uk_users_email` (`email`);",
        "ALTER TABLE `orders` ADD INDEX `idx_orders_user` (`user_id` ASC) USING BTREE;",
        "ALTER TABLE `orders` ADD CONSTRAINT `rel_orders_to_users` "
            + "FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);");
    assertThat(mermaid.schemaId()).isEqualTo(schemaId);
    assertThat(mermaid.mermaid()).contains(
        "T1[\"orders\"]",
        "T2[\"users\"]",
        "BIGINT order_id PK \"NOT NULL; AUTO_INCREMENT\"",
        "ENUM status \"ENUM('READY', 'PAID')\"",
        "DECIMAL total \"DECIMAL(10,2)\"",
        "BIGINT user_id FK",
        "BIGINT user_id PK \"NOT NULL; AUTO_INCREMENT\"",
        "VARCHAR email UK \"VARCHAR(255)\"",
        "T2 |o..o{ T1 : \"" + relationship.name() + "\"");
    assertThat(mermaid.currentRevision()).isEqualTo(currentRevision);
  }

  private String createTable(String schemaId, String name) {
    return blockAsRequester(createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        name,
        "utf8mb4",
        "utf8mb4_general_ci",
        null))).result().tableId();
  }

  private String createColumn(
      String tableId,
      String name,
      String dataType,
      Integer length,
      Integer precision,
      Integer scale,
      boolean autoIncrement,
      List<String> values) {
    return blockAsRequester(createColumnUseCase.createColumn(new CreateColumnCommand(
        tableId,
        name,
        dataType,
        length,
        precision,
        scale,
        autoIncrement,
        null,
        null,
        null,
        values))).result().columnId();
  }

  private void createConstraint(
      String tableId,
      String name,
      ConstraintKind kind,
      String columnId) {
    blockAsRequester(createConstraintUseCase.createConstraint(new CreateConstraintCommand(
        tableId,
        name,
        kind,
        null,
        null,
        List.of(new CreateConstraintColumnCommand(columnId, 0)))));
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

  private <T> T blockAsRequester(Mono<T> mono) {
    return mono
        .contextWrite(ProjectAccessRequesterContext.withRequesterId(TEST_REQUESTER_ID))
        .block();
  }

}
