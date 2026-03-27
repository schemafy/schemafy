package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnResult;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.core.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.AddIndexColumnResult;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.ErdTouchedEntity;
import com.schemafy.core.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.in.AddRelationshipColumnResult;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.core.erd.relationship.domain.exception.RelationshipErrorCode;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.exception.TableErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdMutationTargetResolver {

  private final GetSchemaByIdPort getSchemaByIdPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetRelationshipByIdPort getRelationshipByIdPort;
  private final GetRelationshipColumnByIdPort getRelationshipColumnByIdPort;

  Mono<ResolvedErdMutationTarget> resolveBefore(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_SCHEMA -> {
        CreateSchemaCommand command = requirePayload(payload, CreateSchemaCommand.class);
        yield Mono.just(new ResolvedErdMutationTarget(command.projectId(), null, null));
      }
      case CHANGE_SCHEMA_NAME -> {
        ChangeSchemaNameCommand command = requirePayload(payload, ChangeSchemaNameCommand.class);
        yield resolveBySchemaId(command.schemaId(), command.schemaId());
      }
      case DELETE_SCHEMA -> {
        DeleteSchemaCommand command = requirePayload(payload, DeleteSchemaCommand.class);
        yield resolveBySchemaId(command.schemaId(), command.schemaId());
      }
      case CREATE_TABLE -> {
        CreateTableCommand command = requirePayload(payload, CreateTableCommand.class);
        yield resolveSchemaContext(command.schemaId());
      }
      case CHANGE_TABLE_NAME -> {
        ChangeTableNameCommand command = requirePayload(payload, ChangeTableNameCommand.class);
        yield resolveByTableId(command.tableId(), command.tableId());
      }
      case CHANGE_TABLE_META -> {
        ChangeTableMetaCommand command = requirePayload(payload, ChangeTableMetaCommand.class);
        yield resolveByTableId(command.tableId(), command.tableId());
      }
      case CHANGE_TABLE_EXTRA -> {
        ChangeTableExtraCommand command = requirePayload(payload, ChangeTableExtraCommand.class);
        yield resolveByTableId(command.tableId(), command.tableId());
      }
      case DELETE_TABLE -> {
        DeleteTableCommand command = requirePayload(payload, DeleteTableCommand.class);
        yield resolveByTableId(command.tableId(), command.tableId());
      }
      case CREATE_COLUMN -> {
        CreateColumnCommand command = requirePayload(payload, CreateColumnCommand.class);
        yield resolveTableContext(command.tableId());
      }
      case CHANGE_COLUMN_NAME -> {
        ChangeColumnNameCommand command = requirePayload(payload, ChangeColumnNameCommand.class);
        yield resolveByColumnId(command.columnId(), command.columnId());
      }
      case CHANGE_COLUMN_TYPE -> {
        ChangeColumnTypeCommand command = requirePayload(payload, ChangeColumnTypeCommand.class);
        yield resolveByColumnId(command.columnId(), command.columnId());
      }
      case CHANGE_COLUMN_META -> {
        ChangeColumnMetaCommand command = requirePayload(payload, ChangeColumnMetaCommand.class);
        yield resolveByColumnId(command.columnId(), command.columnId());
      }
      case CHANGE_COLUMN_POSITION -> {
        ChangeColumnPositionCommand command = requirePayload(payload, ChangeColumnPositionCommand.class);
        yield resolveByColumnId(command.columnId(), command.columnId());
      }
      case DELETE_COLUMN -> {
        DeleteColumnCommand command = requirePayload(payload, DeleteColumnCommand.class);
        yield resolveByColumnId(command.columnId(), command.columnId());
      }
      case CREATE_CONSTRAINT -> {
        CreateConstraintCommand command = requirePayload(payload, CreateConstraintCommand.class);
        yield resolveTableContext(command.tableId());
      }
      case CHANGE_CONSTRAINT_NAME -> {
        ChangeConstraintNameCommand command = requirePayload(payload, ChangeConstraintNameCommand.class);
        yield resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case CHANGE_CONSTRAINT_CHECK_EXPR -> {
        ChangeConstraintCheckExprCommand command = requirePayload(payload, ChangeConstraintCheckExprCommand.class);
        yield resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case CHANGE_CONSTRAINT_DEFAULT_EXPR -> {
        ChangeConstraintDefaultExprCommand command = requirePayload(payload, ChangeConstraintDefaultExprCommand.class);
        yield resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case DELETE_CONSTRAINT -> {
        DeleteConstraintCommand command = requirePayload(payload, DeleteConstraintCommand.class);
        yield resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case ADD_CONSTRAINT_COLUMN -> {
        AddConstraintColumnCommand command = requirePayload(payload, AddConstraintColumnCommand.class);
        yield resolveByConstraintId(command.constraintId(), null);
      }
      case REMOVE_CONSTRAINT_COLUMN -> {
        RemoveConstraintColumnCommand command = requirePayload(payload, RemoveConstraintColumnCommand.class);
        yield resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
      }
      case CHANGE_CONSTRAINT_COLUMN_POSITION -> {
        ChangeConstraintColumnPositionCommand command = requirePayload(payload,
            ChangeConstraintColumnPositionCommand.class);
        yield resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
      }
      case CREATE_INDEX -> {
        CreateIndexCommand command = requirePayload(payload, CreateIndexCommand.class);
        yield resolveTableContext(command.tableId());
      }
      case CHANGE_INDEX_NAME -> {
        ChangeIndexNameCommand command = requirePayload(payload, ChangeIndexNameCommand.class);
        yield resolveByIndexId(command.indexId(), command.indexId());
      }
      case CHANGE_INDEX_TYPE -> {
        ChangeIndexTypeCommand command = requirePayload(payload, ChangeIndexTypeCommand.class);
        yield resolveByIndexId(command.indexId(), command.indexId());
      }
      case DELETE_INDEX -> {
        DeleteIndexCommand command = requirePayload(payload, DeleteIndexCommand.class);
        yield resolveByIndexId(command.indexId(), command.indexId());
      }
      case ADD_INDEX_COLUMN -> {
        AddIndexColumnCommand command = requirePayload(payload, AddIndexColumnCommand.class);
        yield resolveByIndexId(command.indexId(), null);
      }
      case REMOVE_INDEX_COLUMN -> {
        RemoveIndexColumnCommand command = requirePayload(payload, RemoveIndexColumnCommand.class);
        yield resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      }
      case CHANGE_INDEX_COLUMN_POSITION -> {
        ChangeIndexColumnPositionCommand command = requirePayload(payload,
            ChangeIndexColumnPositionCommand.class);
        yield resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      }
      case CHANGE_INDEX_COLUMN_SORT_DIRECTION -> {
        ChangeIndexColumnSortDirectionCommand command = requirePayload(payload,
            ChangeIndexColumnSortDirectionCommand.class);
        yield resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      }
      case CREATE_RELATIONSHIP -> {
        CreateRelationshipCommand command = requirePayload(payload, CreateRelationshipCommand.class);
        yield resolveTableContext(command.fkTableId());
      }
      case CHANGE_RELATIONSHIP_NAME -> {
        ChangeRelationshipNameCommand command = requirePayload(payload, ChangeRelationshipNameCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case CHANGE_RELATIONSHIP_KIND -> {
        ChangeRelationshipKindCommand command = requirePayload(payload, ChangeRelationshipKindCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case CHANGE_RELATIONSHIP_CARDINALITY -> {
        ChangeRelationshipCardinalityCommand command = requirePayload(payload,
            ChangeRelationshipCardinalityCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case CHANGE_RELATIONSHIP_EXTRA -> {
        ChangeRelationshipExtraCommand command = requirePayload(payload, ChangeRelationshipExtraCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case DELETE_RELATIONSHIP -> {
        DeleteRelationshipCommand command = requirePayload(payload, DeleteRelationshipCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case ADD_RELATIONSHIP_COLUMN -> {
        AddRelationshipColumnCommand command = requirePayload(payload, AddRelationshipColumnCommand.class);
        yield resolveByRelationshipId(command.relationshipId(), null);
      }
      case REMOVE_RELATIONSHIP_COLUMN -> {
        RemoveRelationshipColumnCommand command = requirePayload(payload, RemoveRelationshipColumnCommand.class);
        yield resolveByRelationshipColumnId(command.relationshipColumnId(), command.relationshipColumnId());
      }
      case CHANGE_RELATIONSHIP_COLUMN_POSITION -> {
        ChangeRelationshipColumnPositionCommand command = requirePayload(payload,
            ChangeRelationshipColumnPositionCommand.class);
        yield resolveByRelationshipColumnId(command.relationshipColumnId(), command.relationshipColumnId());
      }
    };
  }

  <T> FinalizedErdMutationTarget finalizeTarget(
      ErdOperationType operationType,
      ResolvedErdMutationTarget resolvedTarget,
      MutationResult<T> mutationResult) {
    String resolvedSchemaId = resolvedTarget.schemaId();
    String touchedEntityId = operationType.createsEntity()
        ? resolveCreatedEntityId(operationType, mutationResult)
        : resolvedTarget.touchedEntityId();

    if (resolvedSchemaId == null && operationType == ErdOperationType.CREATE_SCHEMA) {
      resolvedSchemaId = touchedEntityId;
    }

    return new FinalizedErdMutationTarget(
        resolvedTarget.projectId(),
        resolvedSchemaId,
        touchedEntityId == null
            ? null
            : new ErdTouchedEntity(operationType.touchedEntityType(), touchedEntityId));
  }

  private <T> String resolveCreatedEntityId(ErdOperationType operationType, MutationResult<T> mutationResult) {
    Object result = mutationResult.result();
    return switch (operationType) {
      case CREATE_SCHEMA -> requireResult(result, CreateSchemaResult.class).id();
      case CREATE_TABLE -> requireResult(result, CreateTableResult.class).tableId();
      case CREATE_COLUMN -> requireResult(result, CreateColumnResult.class).columnId();
      case CREATE_CONSTRAINT -> requireResult(result, CreateConstraintResult.class).constraintId();
      case ADD_CONSTRAINT_COLUMN -> requireResult(result, AddConstraintColumnResult.class).constraintColumnId();
      case CREATE_INDEX -> requireResult(result, CreateIndexResult.class).indexId();
      case ADD_INDEX_COLUMN -> requireResult(result, AddIndexColumnResult.class).indexColumnId();
      case CREATE_RELATIONSHIP -> requireResult(result, CreateRelationshipResult.class).relationshipId();
      case ADD_RELATIONSHIP_COLUMN -> requireResult(result, AddRelationshipColumnResult.class).relationshipColumnId();
      default -> throw new IllegalArgumentException("Unsupported create operation: " + operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveBySchemaId(String schemaId, String touchedEntityId) {
    return getSchemaByIdPort.findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + schemaId)))
        .map(schema -> new ResolvedErdMutationTarget(schema.projectId(), schema.id(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveSchemaContext(String schemaId) {
    return resolveBySchemaId(schemaId, null);
  }

  private Mono<ResolvedErdMutationTarget> resolveByTableId(String tableId, String touchedEntityId) {
    return getTableByIdPort.findTableById(tableId)
        .switchIfEmpty(Mono.error(new DomainException(TableErrorCode.NOT_FOUND, "Table not found: " + tableId)))
        .flatMap(table -> resolveSchemaContext(table.schemaId())
            .map(resolvedTarget -> resolvedTarget.withTouchedEntityId(touchedEntityId)));
  }

  private Mono<ResolvedErdMutationTarget> resolveTableContext(String tableId) {
    return resolveByTableId(tableId, null);
  }

  private Mono<ResolvedErdMutationTarget> resolveByColumnId(String columnId, String touchedEntityId) {
    return getColumnByIdPort.findColumnById(columnId)
        .switchIfEmpty(Mono.error(new DomainException(ColumnErrorCode.NOT_FOUND, "Column not found: " + columnId)))
        .flatMap(column -> resolveByTableId(column.tableId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByConstraintId(String constraintId, String touchedEntityId) {
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(
            new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found: " + constraintId)))
        .flatMap(constraint -> resolveByTableId(constraint.tableId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByConstraintColumnId(
      String constraintColumnId,
      String touchedEntityId) {
    return getConstraintColumnByIdPort.findConstraintColumnById(constraintColumnId)
        .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.COLUMN_NOT_FOUND,
            "Constraint column not found: " + constraintColumnId)))
        .flatMap(constraintColumn -> resolveByConstraintId(constraintColumn.constraintId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByIndexId(String indexId, String touchedEntityId) {
    return getIndexByIdPort.findIndexById(indexId)
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found: " + indexId)))
        .flatMap(index -> resolveByTableId(index.tableId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByIndexColumnId(String indexColumnId, String touchedEntityId) {
    return getIndexColumnByIdPort.findIndexColumnById(indexColumnId)
        .switchIfEmpty(Mono.error(
            new DomainException(IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found: " + indexColumnId)))
        .flatMap(indexColumn -> resolveByIndexId(indexColumn.indexId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByRelationshipId(String relationshipId, String touchedEntityId) {
    return getRelationshipByIdPort.findRelationshipById(relationshipId)
        .switchIfEmpty(Mono.error(
            new DomainException(RelationshipErrorCode.NOT_FOUND, "Relationship not found: " + relationshipId)))
        .flatMap(relationship -> resolveByTableId(relationship.fkTableId(), touchedEntityId));
  }

  private Mono<ResolvedErdMutationTarget> resolveByRelationshipColumnId(
      String relationshipColumnId,
      String touchedEntityId) {
    return getRelationshipColumnByIdPort.findRelationshipColumnById(relationshipColumnId)
        .switchIfEmpty(Mono.error(new DomainException(RelationshipErrorCode.COLUMN_NOT_FOUND,
            "Relationship column not found: " + relationshipColumnId)))
        .flatMap(relationshipColumn -> resolveByRelationshipId(relationshipColumn.relationshipId(), touchedEntityId));
  }

  private static <T> T requirePayload(Object payload, Class<T> type) {
    if (!type.isInstance(payload)) {
      throw new IllegalArgumentException(
          "Unexpected payload type for %s: %s".formatted(type.getSimpleName(), describeType(payload)));
    }
    return type.cast(payload);
  }

  private static <T> T requireResult(Object result, Class<T> type) {
    if (!type.isInstance(result)) {
      throw new IllegalArgumentException(
          "Unexpected result type for %s: %s".formatted(type.getSimpleName(), describeType(result)));
    }
    return type.cast(result);
  }

  private static String describeType(Object value) {
    return value == null ? "null" : value.getClass().getName();
  }

  record ResolvedErdMutationTarget(
      String projectId,
      String schemaId,
      String touchedEntityId) {

    ResolvedErdMutationTarget {
      projectId = Objects.requireNonNull(projectId, "projectId");
    }

    ResolvedErdMutationTarget withTouchedEntityId(String nextTouchedEntityId) {
      return new ResolvedErdMutationTarget(projectId, schemaId, nextTouchedEntityId);
    }
  }

  record FinalizedErdMutationTarget(
      String projectId,
      String schemaId,
      ErdTouchedEntity touchedEntity) {
  }

}
