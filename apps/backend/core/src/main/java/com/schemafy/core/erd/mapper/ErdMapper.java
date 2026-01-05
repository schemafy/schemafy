package com.schemafy.core.erd.mapper;

import com.schemafy.core.erd.repository.entity.Column;
import com.schemafy.core.erd.repository.entity.Constraint;
import com.schemafy.core.erd.repository.entity.ConstraintColumn;
import com.schemafy.core.erd.repository.entity.Index;
import com.schemafy.core.erd.repository.entity.IndexColumn;
import com.schemafy.core.erd.repository.entity.Relationship;
import com.schemafy.core.erd.repository.entity.RelationshipColumn;
import com.schemafy.core.erd.repository.entity.Schema;
import com.schemafy.core.erd.repository.entity.Table;

import validation.Validation;

public final class ErdMapper {

  private ErdMapper() {}

  public static Schema toEntity(Validation.Schema schema) {
    return Schema.builder()
        .projectId(schema.getProjectId())
        .dbVendorId(schema.getDbVendorId().name())
        .name(schema.getName())
        .charset(schema.getCharset())
        .collation(schema.getCollation())
        .vendorOption(schema.getVendorOption())
        .build();
  }

  public static Table toEntity(Validation.Table table) {
    return toEntity(table, null);
  }

  public static Table toEntity(Validation.Table table, String extra) {
    return Table.builder()
        .schemaId(table.getSchemaId())
        .name(table.getName())
        .comment(table.getComment())
        .tableOptions(table.getTableOptions())
        .extra(extra)
        .build();
  }

  public static Column toEntity(Validation.Column column) {
    return Column.builder()
        .tableId(column.getTableId())
        .name(column.getName())
        .seqNo(column.getSeqNo())
        .dataType(column.getDataType())
        .lengthScale(column.getLengthScale())
        .isAutoIncrement(column.getIsAutoIncrement())
        .charset(column.getCharset())
        .collation(column.getCollation())
        .comment(column.getComment())
        .build();
  }

  public static Index toEntity(Validation.Index index) {
    return Index.builder()
        .tableId(index.getTableId())
        .name(index.getName())
        .type(index.getType().name())
        .comment(index.hasComment() ? index.getComment() : null)
        .build();
  }

  public static IndexColumn toEntity(Validation.IndexColumn indexColumn) {
    return IndexColumn.builder()
        .indexId(indexColumn.getIndexId())
        .columnId(indexColumn.getColumnId())
        .seqNo((int) indexColumn.getSeqNo())
        .sortDir(indexColumn.getSortDir().name())
        .build();
  }

  public static Constraint toEntity(Validation.Constraint constraint) {
    return Constraint.builder()
        .tableId(constraint.getTableId())
        .name(constraint.getName())
        .kind(constraint.getKind().name())
        .checkExpr(constraint.hasCheckExpr() ? constraint.getCheckExpr()
            : null)
        .defaultExpr(constraint.hasDefaultExpr()
            ? constraint.getDefaultExpr()
            : null)
        .build();
  }

  public static ConstraintColumn toEntity(
      Validation.ConstraintColumn constraintColumn) {
    return ConstraintColumn.builder()
        .constraintId(constraintColumn.getConstraintId())
        .columnId(constraintColumn.getColumnId())
        .seqNo((int) constraintColumn.getSeqNo())
        .build();
  }

  public static Relationship toEntity(Validation.Relationship relationship) {
    return toEntity(relationship, null);
  }

  public static Relationship toEntity(Validation.Relationship relationship,
      String extra) {
    return Relationship.builder()
        .fkTableId(relationship.getFkTableId())
        .pkTableId(relationship.getPkTableId())
        .name(relationship.getName())
        .kind(relationship.getKind().name())
        .cardinality(relationship.getCardinality().name())
        .onDelete(relationship.getOnDelete().name())
        .onUpdate(relationship.getOnUpdate().name())
        .extra(extra)
        .build();
  }

  public static RelationshipColumn toEntity(
      Validation.RelationshipColumn relationshipColumn) {
    return RelationshipColumn.builder()
        .relationshipId(relationshipColumn.getRelationshipId())
        .fkColumnId(relationshipColumn.getFkColumnId())
        .pkColumnId(relationshipColumn.getPkColumnId())
        .seqNo((int) relationshipColumn.getSeqNo())
        .build();
  }

}
