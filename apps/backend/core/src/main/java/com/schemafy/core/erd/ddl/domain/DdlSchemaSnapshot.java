package com.schemafy.core.erd.ddl.domain;

import java.util.List;

import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record DdlSchemaSnapshot(
    SchemaSnapshot schema,
    List<TableSnapshot> tables) {

  public DdlSchemaSnapshot {
    tables = List.copyOf(tables == null ? List.of() : tables);
  }

  public record SchemaSnapshot(
      String id,
      String dbVendorName,
      String name,
      String charset,
      String collation) {
  }

  public record TableSnapshot(
      Table table,
      List<Column> columns,
      List<ConstraintSnapshot> constraints,
      List<RelationshipSnapshot> relationships,
      List<IndexSnapshot> indexes) {

    public TableSnapshot {
      columns = List.copyOf(columns == null ? List.of() : columns);
      constraints = List.copyOf(constraints == null ? List.of() : constraints);
      relationships = List.copyOf(relationships == null ? List.of() : relationships);
      indexes = List.copyOf(indexes == null ? List.of() : indexes);
    }

  }

  public record Table(
      String id,
      String schemaId,
      String name,
      String charset,
      String collation) {
  }

  public record Column(
      String id,
      String tableId,
      String name,
      String dataType,
      ColumnTypeArguments typeArguments,
      int seqNo,
      boolean autoIncrement,
      String charset,
      String collation,
      String comment) {
  }

  public record ConstraintSnapshot(
      Constraint constraint,
      List<ConstraintColumn> columns) {

    public ConstraintSnapshot {
      columns = List.copyOf(columns == null ? List.of() : columns);
    }

  }

  public record Constraint(
      String id,
      String tableId,
      String name,
      ConstraintKind kind,
      String checkExpr,
      String defaultExpr) {
  }

  public record ConstraintColumn(
      String id,
      String constraintId,
      String columnId,
      int seqNo) {
  }

  public record IndexSnapshot(
      Index index,
      List<IndexColumn> columns) {

    public IndexSnapshot {
      columns = List.copyOf(columns == null ? List.of() : columns);
    }

  }

  public record Index(
      String id,
      String tableId,
      String name,
      IndexType type) {
  }

  public record IndexColumn(
      String id,
      String indexId,
      String columnId,
      int seqNo,
      SortDirection sortDirection) {
  }

  public record RelationshipSnapshot(
      Relationship relationship,
      List<RelationshipColumn> columns) {

    public RelationshipSnapshot {
      columns = List.copyOf(columns == null ? List.of() : columns);
    }

  }

  public record Relationship(
      String id,
      String pkTableId,
      String fkTableId,
      String name,
      RelationshipKind kind,
      Cardinality cardinality,
      String onDelete,
      String onUpdate) {
  }

  public record RelationshipColumn(
      String id,
      String relationshipId,
      String pkColumnId,
      String fkColumnId,
      int seqNo) {
  }

}
