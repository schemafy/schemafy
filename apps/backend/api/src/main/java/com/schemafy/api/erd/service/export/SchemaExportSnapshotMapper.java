package com.schemafy.api.erd.service.export;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintColumnResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintResponse;
import com.schemafy.api.erd.controller.dto.response.ConstraintSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.IndexColumnResponse;
import com.schemafy.api.erd.controller.dto.response.IndexResponse;
import com.schemafy.api.erd.controller.dto.response.IndexSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipColumnResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipResponse;
import com.schemafy.api.erd.controller.dto.response.RelationshipSnapshotResponse;
import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Column;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Constraint;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.ConstraintSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Index;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.IndexColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.IndexSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Relationship;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipColumn;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.RelationshipSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.SchemaSnapshot;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.Table;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot.TableSnapshot;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;

@Component
public class SchemaExportSnapshotMapper {

  public SchemaExportSnapshot toSnapshot(
      SchemaResponse schema,
      Iterable<TableSnapshotResponse> tables,
      String dbVendorName) {
    return new SchemaExportSnapshot(
        toSchemaSnapshot(schema, dbVendorName),
        toTableSnapshots(tables));
  }

  private SchemaSnapshot toSchemaSnapshot(SchemaResponse schema,
      String dbVendorName) {
    return new SchemaSnapshot(
        schema.id(),
        dbVendorName,
        schema.name(),
        schema.charset(),
        schema.collation());
  }

  private List<TableSnapshot> toTableSnapshots(
      Iterable<TableSnapshotResponse> tables) {
    List<TableSnapshot> snapshots = new ArrayList<>();
    if (tables == null) {
      return snapshots;
    }
    for (TableSnapshotResponse table : tables) {
      snapshots.add(toTableSnapshot(table));
    }
    return snapshots;
  }

  private TableSnapshot toTableSnapshot(TableSnapshotResponse snapshot) {
    return new TableSnapshot(
        toTable(snapshot.table()),
        safeList(snapshot.columns()).stream()
            .map(this::toColumn)
            .toList(),
        safeList(snapshot.constraints()).stream()
            .map(this::toConstraintSnapshot)
            .toList(),
        safeList(snapshot.relationships()).stream()
            .map(this::toRelationshipSnapshot)
            .toList(),
        safeList(snapshot.indexes()).stream()
            .map(this::toIndexSnapshot)
            .toList());
  }

  private Table toTable(TableResponse table) {
    return new Table(
        table.id(),
        table.schemaId(),
        table.name(),
        table.charset(),
        table.collation());
  }

  private Column toColumn(ColumnResponse column) {
    return new Column(
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

  private ConstraintSnapshot toConstraintSnapshot(
      ConstraintSnapshotResponse snapshot) {
    return new ConstraintSnapshot(
        toConstraint(snapshot.constraint()),
        safeList(snapshot.columns()).stream()
            .map(this::toConstraintColumn)
            .toList());
  }

  private Constraint toConstraint(ConstraintResponse constraint) {
    return new Constraint(
        constraint.id(),
        constraint.tableId(),
        constraint.name(),
        constraint.kind(),
        constraint.checkExpr(),
        constraint.defaultExpr());
  }

  private ConstraintColumn toConstraintColumn(
      ConstraintColumnResponse column) {
    return new ConstraintColumn(
        column.id(),
        column.constraintId(),
        column.columnId(),
        column.seqNo());
  }

  private IndexSnapshot toIndexSnapshot(IndexSnapshotResponse snapshot) {
    return new IndexSnapshot(
        toIndex(snapshot.index()),
        safeList(snapshot.columns()).stream()
            .map(this::toIndexColumn)
            .toList());
  }

  private Index toIndex(IndexResponse index) {
    return new Index(
        index.id(),
        index.tableId(),
        index.name(),
        index.type());
  }

  private IndexColumn toIndexColumn(IndexColumnResponse column) {
    return new IndexColumn(
        column.id(),
        column.indexId(),
        column.columnId(),
        column.seqNo(),
        column.sortDirection());
  }

  private RelationshipSnapshot toRelationshipSnapshot(
      RelationshipSnapshotResponse snapshot) {
    return new RelationshipSnapshot(
        toRelationship(snapshot.relationship()),
        safeList(snapshot.columns()).stream()
            .map(this::toRelationshipColumn)
            .toList());
  }

  private Relationship toRelationship(RelationshipResponse relationship) {
    JsonNode extra = relationship.extra();
    return new Relationship(
        relationship.id(),
        relationship.pkTableId(),
        relationship.fkTableId(),
        relationship.name(),
        relationship.kind(),
        relationship.cardinality(),
        textExtra(extra, "onDelete"),
        textExtra(extra, "onUpdate"));
  }

  private RelationshipColumn toRelationshipColumn(
      RelationshipColumnResponse column) {
    return new RelationshipColumn(
        column.id(),
        column.relationshipId(),
        column.pkColumnId(),
        column.fkColumnId(),
        column.seqNo());
  }

  private String textExtra(JsonNode extra, String fieldName) {
    if (extra == null || extra.isNull() || !extra.has(fieldName)
        || extra.get(fieldName).isNull()) {
      return null;
    }
    JsonNode value = extra.get(fieldName);
    if (!value.isTextual()) {
      throw new DomainException(RelationshipErrorCode.INVALID_VALUE,
          "Relationship extra '%s' must be a string".formatted(fieldName));
    }
    return value.asText();
  }

  private static <T> List<T> safeList(List<T> value) {
    return value == null ? List.of() : value;
  }

}
