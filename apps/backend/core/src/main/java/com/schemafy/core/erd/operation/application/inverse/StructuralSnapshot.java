package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.index.domain.type.SortDirection;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;

public record StructuralSnapshot(
    String schemaId,
    List<ColumnSnapshot> columns,
    List<ConstraintSnapshot> constraints,
    List<ConstraintColumnSnapshot> constraintColumns,
    List<IndexSnapshot> indexes,
    List<IndexColumnSnapshot> indexColumns,
    List<RelationshipSnapshot> relationships,
    List<RelationshipColumnSnapshot> relationshipColumns) {

  public StructuralSnapshot {
    columns = List.copyOf(columns == null ? List.of() : columns);
    constraints = List.copyOf(constraints == null ? List.of() : constraints);
    constraintColumns = List.copyOf(constraintColumns == null ? List.of() : constraintColumns);
    indexes = List.copyOf(indexes == null ? List.of() : indexes);
    indexColumns = List.copyOf(indexColumns == null ? List.of() : indexColumns);
    relationships = List.copyOf(relationships == null ? List.of() : relationships);
    relationshipColumns = List.copyOf(relationshipColumns == null ? List.of() : relationshipColumns);
  }

  public record ColumnSnapshot(
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

    public static ColumnSnapshot from(Column column) {
      return new ColumnSnapshot(
          column.id(),
          column.tableId(),
          column.name(),
          column.dataType(),
          column.typeArguments(),
          column.seqNo(),
          column.autoIncrement(),
          column.charset(),
          column.collation(),
          column.comment());
    }

    public Column toDomain() {
      return new Column(id, tableId, name, dataType, typeArguments, seqNo, autoIncrement, charset, collation, comment);
    }
  }

  public record ConstraintSnapshot(
      String id,
      String tableId,
      String name,
      ConstraintKind kind,
      String checkExpr,
      String defaultExpr) {

    public static ConstraintSnapshot from(Constraint constraint) {
      return new ConstraintSnapshot(
          constraint.id(),
          constraint.tableId(),
          constraint.name(),
          constraint.kind(),
          constraint.checkExpr(),
          constraint.defaultExpr());
    }

    public Constraint toDomain() {
      return new Constraint(id, tableId, name, kind, checkExpr, defaultExpr);
    }
  }

  public record ConstraintColumnSnapshot(
      String id,
      String constraintId,
      String columnId,
      int seqNo) {

    public static ConstraintColumnSnapshot from(ConstraintColumn column) {
      return new ConstraintColumnSnapshot(column.id(), column.constraintId(), column.columnId(), column.seqNo());
    }

    public ConstraintColumn toDomain() {
      return new ConstraintColumn(id, constraintId, columnId, seqNo);
    }
  }

  public record IndexSnapshot(
      String id,
      String tableId,
      String name,
      IndexType type) {

    public static IndexSnapshot from(Index index) {
      return new IndexSnapshot(index.id(), index.tableId(), index.name(), index.type());
    }

    public Index toDomain() {
      return new Index(id, tableId, name, type);
    }
  }

  public record IndexColumnSnapshot(
      String id,
      String indexId,
      String columnId,
      int seqNo,
      SortDirection sortDirection) {

    public static IndexColumnSnapshot from(IndexColumn column) {
      return new IndexColumnSnapshot(
          column.id(), column.indexId(), column.columnId(), column.seqNo(), column.sortDirection());
    }

    public IndexColumn toDomain() {
      return new IndexColumn(id, indexId, columnId, seqNo, sortDirection);
    }
  }

  public record RelationshipSnapshot(
      String id,
      String pkTableId,
      String fkTableId,
      String name,
      RelationshipKind kind,
      Cardinality cardinality,
      String extra) {

    public static RelationshipSnapshot from(Relationship relationship) {
      return new RelationshipSnapshot(
          relationship.id(),
          relationship.pkTableId(),
          relationship.fkTableId(),
          relationship.name(),
          relationship.kind(),
          relationship.cardinality(),
          relationship.extra());
    }

    public Relationship toDomain() {
      return new Relationship(id, pkTableId, fkTableId, name, kind, cardinality, extra);
    }
  }

  public record RelationshipColumnSnapshot(
      String id,
      String relationshipId,
      String pkColumnId,
      String fkColumnId,
      int seqNo) {

    public static RelationshipColumnSnapshot from(RelationshipColumn column) {
      return new RelationshipColumnSnapshot(
          column.id(), column.relationshipId(), column.pkColumnId(), column.fkColumnId(), column.seqNo());
    }

    public RelationshipColumn toDomain() {
      return new RelationshipColumn(id, relationshipId, pkColumnId, fkColumnId, seqNo);
    }
  }

}
