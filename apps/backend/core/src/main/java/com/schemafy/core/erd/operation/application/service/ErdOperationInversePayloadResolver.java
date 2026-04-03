package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdOperationInversePayloadResolver {

  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;

  Mono<Object> resolveBefore(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
    case CHANGE_TABLE_NAME -> resolveTableNameInverse(payload);
    case CHANGE_COLUMN_NAME -> resolveColumnNameInverse(payload);
    case CHANGE_COLUMN_TYPE -> resolveColumnTypeInverse(payload);
    case CHANGE_RELATIONSHIP_NAME -> resolveRelationshipNameInverse(payload);
    case CHANGE_RELATIONSHIP_KIND -> resolveRelationshipKindInverse(payload);
    case CHANGE_RELATIONSHIP_CARDINALITY -> resolveRelationshipCardinalityInverse(payload);
    case CHANGE_CONSTRAINT_NAME -> resolveConstraintNameInverse(payload);
    case CHANGE_INDEX_NAME -> resolveIndexNameInverse(payload);
    case CHANGE_INDEX_TYPE -> resolveIndexTypeInverse(payload);
    default -> Mono.empty();
    };
  }

  private Mono<Object> resolveTableNameInverse(Object payload) {
    ChangeTableNameCommand command = requirePayload(payload, ChangeTableNameCommand.class);
    return getTableByIdPort.findTableById(command.tableId())
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, tableNotFound(command.tableId()))))
        .map(this::toInverseTableNamePayload);
  }

  private Mono<Object> resolveColumnNameInverse(Object payload) {
    ChangeColumnNameCommand command = requirePayload(payload, ChangeColumnNameCommand.class);
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, columnNotFound(command.columnId()))))
        .map(this::toInverseColumnNamePayload);
  }

  private Mono<Object> resolveColumnTypeInverse(Object payload) {
    ChangeColumnTypeCommand command = requirePayload(payload, ChangeColumnTypeCommand.class);
    return getColumnByIdPort.findColumnById(command.columnId())
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, columnNotFound(command.columnId()))))
        .map(this::toInverseColumnTypePayload);
  }

  private Mono<Object> resolveRelationshipNameInverse(Object payload) {
    ChangeRelationshipNameCommand command = requirePayload(payload, ChangeRelationshipNameCommand.class);
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(this::toInverseRelationshipNamePayload);
  }

  private Mono<Object> resolveRelationshipKindInverse(Object payload) {
    ChangeRelationshipKindCommand command = requirePayload(payload, ChangeRelationshipKindCommand.class);
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(this::toInverseRelationshipKindPayload);
  }

  private Mono<Object> resolveRelationshipCardinalityInverse(Object payload) {
    ChangeRelationshipCardinalityCommand command =
        requirePayload(payload, ChangeRelationshipCardinalityCommand.class);
    return getRelationshipByIdPort.findRelationshipById(command.relationshipId())
        .switchIfEmpty(Mono.error(new DomainException(
            RelationshipErrorCode.NOT_FOUND,
            relationshipNotFound(command.relationshipId()))))
        .map(this::toInverseRelationshipCardinalityPayload);
  }

  private Mono<Object> resolveConstraintNameInverse(Object payload) {
    ChangeConstraintNameCommand command = requirePayload(payload, ChangeConstraintNameCommand.class);
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.NOT_FOUND,
            constraintNotFound(command.constraintId()))))
        .map(this::toInverseConstraintNamePayload);
  }

  private Mono<Object> resolveIndexNameInverse(Object payload) {
    ChangeIndexNameCommand command = requirePayload(payload, ChangeIndexNameCommand.class);
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, indexNotFound(command.indexId()))))
        .map(this::toInverseIndexNamePayload);
  }

  private Mono<Object> resolveIndexTypeInverse(Object payload) {
    ChangeIndexTypeCommand command = requirePayload(payload, ChangeIndexTypeCommand.class);
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, indexNotFound(command.indexId()))))
        .map(this::toInverseIndexTypePayload);
  }

  private Object toInverseTableNamePayload(Table table) {
    return new ChangeTableNameCommand(table.id(), table.name());
  }

  private Object toInverseColumnNamePayload(Column column) {
    return new ChangeColumnNameCommand(column.id(), column.name());
  }

  private Object toInverseColumnTypePayload(Column column) {
    ColumnTypeArguments typeArguments = column.typeArguments();
    return new ChangeColumnTypeCommand(
        column.id(),
        column.dataType(),
        typeArguments == null ? null : typeArguments.length(),
        typeArguments == null ? null : typeArguments.precision(),
        typeArguments == null ? null : typeArguments.scale(),
        typeArguments == null ? null : typeArguments.values());
  }

  private Object toInverseRelationshipNamePayload(Relationship relationship) {
    return new ChangeRelationshipNameCommand(relationship.id(), relationship.name());
  }

  private Object toInverseRelationshipKindPayload(Relationship relationship) {
    return new ChangeRelationshipKindCommand(relationship.id(), relationship.kind());
  }

  private Object toInverseRelationshipCardinalityPayload(Relationship relationship) {
    return new ChangeRelationshipCardinalityCommand(relationship.id(), relationship.cardinality());
  }

  private Object toInverseConstraintNamePayload(Constraint constraint) {
    return new ChangeConstraintNameCommand(constraint.id(), constraint.name());
  }

  private Object toInverseIndexNamePayload(Index index) {
    return new ChangeIndexNameCommand(index.id(), index.name());
  }

  private Object toInverseIndexTypePayload(Index index) {
    return new ChangeIndexTypeCommand(index.id(), index.type());
  }

  private <T> T requirePayload(Object payload, Class<T> type) {
    if (type.isInstance(payload)) {
      return type.cast(payload);
    }
    throw new IllegalArgumentException("Unsupported payload for operation resolver: " + type.getSimpleName());
  }

  private static String tableNotFound(String tableId) {
    return "Table not found: " + tableId;
  }

  private static String columnNotFound(String columnId) {
    return "Column not found: " + columnId;
  }

  private static String relationshipNotFound(String relationshipId) {
    return "Relationship not found: " + relationshipId;
  }

  private static String constraintNotFound(String constraintId) {
    return "Constraint not found: " + constraintId;
  }

  private static String indexNotFound(String indexId) {
    return "Index not found: " + indexId;
  }

}
