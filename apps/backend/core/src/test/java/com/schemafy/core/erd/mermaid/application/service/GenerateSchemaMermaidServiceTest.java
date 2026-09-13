package com.schemafy.core.erd.mermaid.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Column;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Constraint;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Relationship;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Table;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.TableSnapshot;
import com.schemafy.core.erd.mermaid.application.port.in.GenerateSchemaMermaidCommand;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerateSchemaMermaidService")
class GenerateSchemaMermaidServiceTest {

  private final GenerateSchemaMermaidService sut = new GenerateSchemaMermaidService();

  @Test
  @DisplayName("table, column, key와 관계를 결정적인 Mermaid ERD로 생성한다")
  void generatesPhysicalMermaidErd() {
    RelationshipSnapshot relationship = relationship(
        "relationship-1", "table-users", "table-orders",
        "fk_orders_user", RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "column-user-id", "column-order-user-id");
    TableSnapshot users = table(
        "table-users", "users",
        List.of(
            column("column-user-id", "table-users", "id", "BIGINT", 0),
            column("column-user-email", "table-users", "email", "VARCHAR", 1)),
        List.of(
            constraint("pk-users", "table-users", ConstraintKind.PRIMARY_KEY,
                "column-user-id"),
            constraint("uk-users-email", "table-users", ConstraintKind.UNIQUE,
                "column-user-email")),
        List.of(relationship));
    TableSnapshot orders = table(
        "table-orders", "orders",
        List.of(
            column("column-order-id", "table-orders", "id", "BIGINT", 0),
            column("column-order-user-id", "table-orders", "user_id", "BIGINT", 1)),
        List.of(
            constraint("pk-orders", "table-orders", ConstraintKind.PRIMARY_KEY,
                "column-order-id", "column-order-user-id"),
            constraint("uk-orders-user", "table-orders", ConstraintKind.UNIQUE,
                "column-order-user-id")),
        List.of(relationship));
    SchemaExportSnapshot snapshot = schema(users, orders);

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(snapshot)))
        .assertNext(mermaid -> assertThat(mermaid).isEqualTo("""
            erDiagram
                T1["orders"] {
                    BIGINT id PK "NOT NULL"
                    BIGINT user_id PK, FK, UK "NOT NULL"
                }
                T2["users"] {
                    BIGINT id PK "NOT NULL"
                    VARCHAR email UK
                }

                T2 ||..o{ T1 : "fk_orders_user"\
            """))
        .verifyComplete();
  }

  @Test
  @DisplayName("column type argument를 Mermaid attribute에 보존한다")
  void preservesColumnTypeArguments() {
    TableSnapshot products = table(
        "table-products", "products",
        List.of(
            column("column-name", "table-products", "name", "VARCHAR",
                new ColumnTypeArguments(20, null, null), 0),
            column("column-price", "table-products", "price", "DECIMAL",
                new ColumnTypeArguments(null, 10, 2), 1),
            column("column-status", "table-products", "status", "ENUM",
                new ColumnTypeArguments(null, null, null,
                    List.of("DRAFT", "PUBLISHED")),
                2),
            column("column-flags", "table-products", "flags", "SET",
                new ColumnTypeArguments(null, null, null,
                    List.of("FEATURED", "ARCHIVED")),
                3)),
        List.of(constraint("uk-products-name", "table-products",
            ConstraintKind.UNIQUE, "column-name")),
        List.of());

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(products))))
        .assertNext(mermaid -> assertThat(mermaid).isEqualTo("""
            erDiagram
                T1["products"] {
                    VARCHAR name UK "VARCHAR(20)"
                    DECIMAL price "DECIMAL(10,2)"
                    ENUM status "ENUM('DRAFT', 'PUBLISHED')"
                    SET flags "SET('FEATURED', 'ARCHIVED')"
                }\
            """))
        .verifyComplete();
  }

  @Test
  @DisplayName("NOT NULL과 AUTO_INCREMENT를 Mermaid attribute에 표시한다")
  void includesColumnOptions() {
    TableSnapshot users = table(
        "table-users", "users",
        List.of(
            column("column-id", "table-users", "id", "BIGINT",
                null, 0, true),
            column("column-email", "table-users", "email", "VARCHAR",
                new ColumnTypeArguments(255, null, null), 1),
            column("column-nickname", "table-users", "nickname", "VARCHAR",
                new ColumnTypeArguments(50, null, null), 2)),
        List.of(
            constraint("pk-users", "table-users", ConstraintKind.PRIMARY_KEY,
                "column-id"),
            constraint("nn-users-email", "table-users", ConstraintKind.NOT_NULL,
                "column-email")),
        List.of());

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(users))))
        .assertNext(mermaid -> assertThat(mermaid).isEqualTo("""
            erDiagram
                T1["users"] {
                    BIGINT id PK "NOT NULL; AUTO_INCREMENT"
                    VARCHAR email "VARCHAR(255); NOT NULL"
                    VARCHAR nickname "VARCHAR(50)"
                }\
            """))
        .verifyComplete();
  }

  @Test
  @DisplayName("Mermaid entity code와 실제 문자를 충돌 없이 구분한다")
  void escapesMermaidEntityCodeMarkersWithoutCollision() {
    RelationshipSnapshot relationship = relationship(
        "relationship-1", "table-quote", "table-entity-code",
        "relation\"#34;", RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "column-parent-id", "column-child-parent-id");
    TableSnapshot quoteTable = table(
        "table-quote", "entity\"",
        List.of(column("column-parent-id", "table-quote", "id", "BIGINT", 0)),
        List.of(constraint("pk-parent", "table-quote",
            ConstraintKind.PRIMARY_KEY, "column-parent-id")),
        List.of());
    TableSnapshot entityCodeTable = table(
        "table-entity-code", "entity#34;",
        List.of(
            column("column-child-parent-id", "table-entity-code",
                "parent_id", "BIGINT", 0),
            column("column-status", "table-entity-code", "status", "ENUM",
                new ColumnTypeArguments(null, null, null,
                    List.of("\"", "#34;")),
                1)),
        List.of(),
        List.of(relationship));

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(quoteTable, entityCodeTable))))
        .assertNext(mermaid -> assertThat(mermaid).isEqualTo("""
            erDiagram
                T1["entity#34;"] {
                    BIGINT id PK "NOT NULL"
                }
                T2["entity#35;34;"] {
                    BIGINT parent_id FK
                    ENUM status "ENUM('#34;', '#35;34;')"
                }

                T1 |o..o{ T2 : "relation#34;#35;34;"\
            """))
        .verifyComplete();
  }

  @Test
  @DisplayName("nullable self relationship을 선택적 1:1 identifying 관계로 생성한다")
  void generatesNullableSelfRelationship() {
    RelationshipSnapshot managerRelationship = relationship(
        "relationship-manager", "table-employees", "table-employees",
        "manager", RelationshipKind.IDENTIFYING,
        Cardinality.ONE_TO_ONE,
        "column-employee-id", "column-manager-id");
    TableSnapshot employees = table(
        "table-employees", "employees",
        List.of(
            column("column-employee-id", "table-employees", "id", "BIGINT", 0),
            column("column-manager-id", "table-employees", "manager_id", "BIGINT", 1)),
        List.of(constraint("pk-employees", "table-employees",
            ConstraintKind.PRIMARY_KEY, "column-employee-id")),
        List.of(managerRelationship));

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(employees))))
        .assertNext(mermaid -> assertThat(mermaid).isEqualTo("""
            erDiagram
                T1["employees"] {
                    BIGINT id PK "NOT NULL"
                    BIGINT manager_id FK
                }

                T1 |o--o| T1 : "manager"\
            """))
        .verifyComplete();
  }

  @Test
  @DisplayName("빈 schema와 컬럼이 없는 table도 Mermaid entity로 생성한다")
  void generatesEmptySchemaAndTable() {
    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema())))
        .expectNext("erDiagram")
        .verifyComplete();

    TableSnapshot emptyTable = table(
        "table-empty", "empty\"table", List.of(), List.of(), List.of());
    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(emptyTable))))
        .expectNext("erDiagram\n    T1[\"empty#34;table\"]")
        .verifyComplete();
  }

  @Test
  @DisplayName("relationship 대상 table이 snapshot에 없으면 예외가 발생한다")
  void throwsWhenRelationshipTableIsMissing() {
    RelationshipSnapshot relationship = relationship(
        "relationship-1", "table-missing", "table-orders",
        "fk_orders_missing", RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "column-missing-id", "column-order-user-id");
    TableSnapshot orders = table(
        "table-orders", "orders",
        List.of(column("column-order-user-id", "table-orders", "user_id",
            "BIGINT", 0)),
        List.of(),
        List.of(relationship));

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(orders))))
        .expectErrorMatches(error -> error instanceof DomainException exception
            && exception.getErrorCode() == RelationshipErrorCode.TARGET_TABLE_NOT_FOUND)
        .verify();
  }

  @Test
  @DisplayName("relationship 대상 column이 snapshot에 없으면 예외가 발생한다")
  void throwsWhenRelationshipColumnIsMissing() {
    RelationshipSnapshot relationship = relationship(
        "relationship-1", "table-users", "table-orders",
        "fk_orders_user", RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        "column-user-missing", "column-order-user-id");
    TableSnapshot users = table(
        "table-users", "users",
        List.of(column("column-user-id", "table-users", "id", "BIGINT", 0)),
        List.of(),
        List.of(relationship));
    TableSnapshot orders = table(
        "table-orders", "orders",
        List.of(column("column-order-user-id", "table-orders", "user_id",
            "BIGINT", 0)),
        List.of(),
        List.of(relationship));

    StepVerifier.create(sut.generateSchemaMermaid(
        new GenerateSchemaMermaidCommand(schema(users, orders))))
        .expectErrorMatches(error -> error instanceof DomainException exception
            && exception.getErrorCode() == RelationshipErrorCode.COLUMN_NOT_FOUND)
        .verify();
  }

  private SchemaExportSnapshot schema(TableSnapshot... tables) {
    return new SchemaExportSnapshot(
        new SchemaSnapshot("schema-1", "mysql", "app", null, null),
        List.of(tables));
  }

  private TableSnapshot table(
      String id,
      String name,
      List<Column> columns,
      List<ConstraintSnapshot> constraints,
      List<RelationshipSnapshot> relationships) {
    return new TableSnapshot(
        new Table(id, "schema-1", name, null, null),
        columns,
        constraints,
        relationships,
        List.of());
  }

  private Column column(String id, String tableId, String name,
      String dataType, int seqNo) {
    return column(id, tableId, name, dataType, null, seqNo);
  }

  private Column column(String id, String tableId, String name,
      String dataType, ColumnTypeArguments typeArguments, int seqNo) {
    return column(id, tableId, name, dataType, typeArguments, seqNo, false);
  }

  private Column column(String id, String tableId, String name,
      String dataType, ColumnTypeArguments typeArguments, int seqNo,
      boolean autoIncrement) {
    return new Column(
        id, tableId, name, dataType, typeArguments, seqNo, autoIncrement,
        null, null, null);
  }

  private ConstraintSnapshot constraint(
      String id,
      String tableId,
      ConstraintKind kind,
      String... columnIds) {
    List<ConstraintColumn> columns = java.util.stream.IntStream
        .range(0, columnIds.length)
        .mapToObj(index -> new ConstraintColumn(
            id + "-column-" + index,
            id,
            columnIds[index],
            index))
        .toList();
    return new ConstraintSnapshot(
        new Constraint(id, tableId, id, kind, null, null),
        columns);
  }

  private RelationshipSnapshot relationship(
      String id,
      String pkTableId,
      String fkTableId,
      String name,
      RelationshipKind kind,
      Cardinality cardinality,
      String pkColumnId,
      String fkColumnId) {
    return new RelationshipSnapshot(
        new Relationship(
            id, pkTableId, fkTableId, name, kind, cardinality, null, null),
        List.of(new RelationshipColumn(
            id + "-column", id, pkColumnId, fkColumnId, 0)));
  }

}
