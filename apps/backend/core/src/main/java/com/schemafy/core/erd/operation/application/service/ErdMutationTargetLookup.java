package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdMutationTargetLookup {

  private final GetSchemaByIdPort getSchemaByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  Mono<ResolvedErdMutationTarget> resolveBySchemaId(String schemaId, String touchedEntityId) {
    return getSchemaByIdPort.findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + schemaId)))
        .map(schema -> new ResolvedErdMutationTarget(schema.projectId(), schema.id(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveSchemaContext(String schemaId) {
    return resolveBySchemaId(schemaId, null);
  }

  Mono<ResolvedErdMutationTarget> resolveByTableId(String tableId, String touchedEntityId) {
    return getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + tableId)))
        .flatMap(table -> resolveSchemaContext(table.schemaId())
            .map(resolvedTarget -> resolvedTarget.withTouchedEntityId(touchedEntityId)));
  }

  Mono<ResolvedErdMutationTarget> resolveTableContext(String tableId) {
    return resolveByTableId(tableId, null);
  }

  Mono<ResolvedErdMutationTarget> resolveByColumnId(String columnId, String touchedEntityId) {
    return getColumnByIdPort.findColumnById(columnId)
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + columnId)))
        .flatMap(column -> resolveByTableId(column.tableId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByConstraintId(String constraintId, String touchedEntityId) {
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(
            new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found: " + constraintId)))
        .flatMap(constraint -> resolveByTableId(constraint.tableId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByConstraintColumnId(
      String constraintColumnId,
      String touchedEntityId) {
    return getConstraintColumnByIdPort.findConstraintColumnById(constraintColumnId)
        .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.COLUMN_NOT_FOUND,
            "Constraint column not found: " + constraintColumnId)))
        .flatMap(constraintColumn -> resolveByConstraintId(constraintColumn.constraintId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByIndexId(String indexId, String touchedEntityId) {
    return getIndexByIdPort.findIndexById(indexId)
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found: " + indexId)))
        .flatMap(index -> resolveByTableId(index.tableId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByIndexColumnId(String indexColumnId, String touchedEntityId) {
    return getIndexColumnByIdPort.findIndexColumnById(indexColumnId)
        .switchIfEmpty(Mono.error(
            new DomainException(IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found: " + indexColumnId)))
        .flatMap(indexColumn -> resolveByIndexId(indexColumn.indexId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByRelationshipId(String relationshipId, String touchedEntityId) {
    return getRelationshipByIdPort.findRelationshipById(relationshipId)
        .switchIfEmpty(Mono.error(
            new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found: " + relationshipId)))
        .flatMap(relationship -> resolveByTableId(relationship.fkTableId(), touchedEntityId));
  }

  Mono<ResolvedErdMutationTarget> resolveByRelationshipColumnId(
      String relationshipColumnId,
      String touchedEntityId) {
    return getRelationshipColumnByIdPort.findRelationshipColumnById(relationshipColumnId)
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found: " + relationshipColumnId)))
        .flatMap(relationshipColumn -> resolveByRelationshipId(relationshipColumn.relationshipId(), touchedEntityId));
  }

}
