package com.schemafy.core.erd.operation.application.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.schemafy.core.erd.column.application.port.in.ChangeColumnMetaCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnNameCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.in.CreateColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintNameCommand;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.core.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.core.erd.index.application.port.in.AddIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexNameCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexTypeCommand;
import com.schemafy.core.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.core.erd.index.application.port.in.DeleteIndexCommand;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeColumnTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeIndexTypeInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipCardinalityInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeRelationshipNameInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeTableNameInverse;
import com.schemafy.core.erd.operation.application.inverse.StructuralOperationInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.in.AddRelationshipColumnCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipCardinalityCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipColumnPositionCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipExtraCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipKindCommand;
import com.schemafy.core.erd.relationship.application.port.in.ChangeRelationshipNameCommand;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.DeleteRelationshipCommand;
import com.schemafy.core.erd.relationship.application.port.in.RemoveRelationshipColumnCommand;
import com.schemafy.core.erd.schema.application.port.in.ChangeSchemaNameCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableExtraCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableMetaCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.DeleteTableCommand;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ErdMutationTargetResolver {

  private final ErdMutationTargetLookup targetLookup;

  Mono<ResolvedErdMutationTarget> resolveBefore(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_SCHEMA, CHANGE_SCHEMA_NAME, DELETE_SCHEMA -> resolveSchemaOperation(operationType, payload);
      case CREATE_TABLE, CHANGE_TABLE_NAME, CHANGE_TABLE_META, CHANGE_TABLE_EXTRA, DELETE_TABLE ->
          resolveTableOperation(operationType, payload);
      case CREATE_COLUMN, CHANGE_COLUMN_NAME, CHANGE_COLUMN_TYPE, CHANGE_COLUMN_META, CHANGE_COLUMN_POSITION,
          DELETE_COLUMN -> resolveColumnOperation(operationType, payload);
      case CREATE_CONSTRAINT, CHANGE_CONSTRAINT_NAME, CHANGE_CONSTRAINT_CHECK_EXPR,
          CHANGE_CONSTRAINT_DEFAULT_EXPR, DELETE_CONSTRAINT, ADD_CONSTRAINT_COLUMN, REMOVE_CONSTRAINT_COLUMN,
          CHANGE_CONSTRAINT_COLUMN_POSITION -> resolveConstraintOperation(operationType, payload);
      case CREATE_INDEX, CHANGE_INDEX_NAME, CHANGE_INDEX_TYPE, DELETE_INDEX, ADD_INDEX_COLUMN, REMOVE_INDEX_COLUMN,
          CHANGE_INDEX_COLUMN_POSITION, CHANGE_INDEX_COLUMN_SORT_DIRECTION -> resolveIndexOperation(
              operationType, payload);
      case CREATE_RELATIONSHIP, CHANGE_RELATIONSHIP_NAME, CHANGE_RELATIONSHIP_KIND,
          CHANGE_RELATIONSHIP_CARDINALITY, CHANGE_RELATIONSHIP_EXTRA, DELETE_RELATIONSHIP, ADD_RELATIONSHIP_COLUMN,
          REMOVE_RELATIONSHIP_COLUMN, CHANGE_RELATIONSHIP_COLUMN_POSITION -> resolveRelationshipOperation(
              operationType, payload);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveSchemaOperation(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_SCHEMA -> {
        CreateSchemaCommand command = requirePayload(payload, CreateSchemaCommand.class);
        yield Mono.just(new ResolvedErdMutationTarget(command.projectId(), null, null));
      }
      case CHANGE_SCHEMA_NAME -> {
        ChangeSchemaNameCommand command = requirePayload(payload, ChangeSchemaNameCommand.class);
        yield targetLookup.resolveBySchemaId(command.schemaId(), command.schemaId());
      }
      case DELETE_SCHEMA -> {
        DeleteSchemaCommand command = requirePayload(payload, DeleteSchemaCommand.class);
        yield targetLookup.resolveBySchemaId(command.schemaId(), command.schemaId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveTableOperation(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_TABLE -> {
        CreateTableCommand command = requirePayload(payload, CreateTableCommand.class);
        yield targetLookup.resolveSchemaContext(command.schemaId());
      }
      case CHANGE_TABLE_NAME -> resolveChangeTableName(payload);
      case CHANGE_TABLE_META -> {
        ChangeTableMetaCommand command = requirePayload(payload, ChangeTableMetaCommand.class);
        yield targetLookup.resolveByTableId(command.tableId(), command.tableId());
      }
      case CHANGE_TABLE_EXTRA -> {
        ChangeTableExtraCommand command = requirePayload(payload, ChangeTableExtraCommand.class);
        yield targetLookup.resolveByTableId(command.tableId(), command.tableId());
      }
      case DELETE_TABLE -> {
        DeleteTableCommand command = requirePayload(payload, DeleteTableCommand.class);
        yield targetLookup.resolveByTableId(command.tableId(), command.tableId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveColumnOperation(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_COLUMN -> {
        CreateColumnCommand command = requirePayload(payload, CreateColumnCommand.class);
        yield targetLookup.resolveTableContext(command.tableId());
      }
      case CHANGE_COLUMN_NAME -> resolveChangeColumnName(payload);
      case CHANGE_COLUMN_TYPE -> resolveChangeColumnType(payload);
      case CHANGE_COLUMN_META -> {
        ChangeColumnMetaCommand command = requirePayload(payload, ChangeColumnMetaCommand.class);
        yield targetLookup.resolveByColumnId(command.columnId(), command.columnId());
      }
      case CHANGE_COLUMN_POSITION -> {
        ChangeColumnPositionCommand command = requirePayload(payload, ChangeColumnPositionCommand.class);
        yield targetLookup.resolveByColumnId(command.columnId(), command.columnId());
      }
      case DELETE_COLUMN -> {
        DeleteColumnCommand command = requirePayload(payload, DeleteColumnCommand.class);
        yield targetLookup.resolveByColumnId(command.columnId(), command.columnId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveConstraintOperation(
      ErdOperationType operationType,
      Object payload) {
    return switch (operationType) {
      case CREATE_CONSTRAINT -> {
        CreateConstraintCommand command = requirePayload(payload, CreateConstraintCommand.class);
        yield targetLookup.resolveTableContext(command.tableId());
      }
      case CHANGE_CONSTRAINT_NAME -> resolveChangeConstraintName(payload);
      case CHANGE_CONSTRAINT_CHECK_EXPR -> {
        ChangeConstraintCheckExprCommand command = requirePayload(payload, ChangeConstraintCheckExprCommand.class);
        yield targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case CHANGE_CONSTRAINT_DEFAULT_EXPR -> {
        ChangeConstraintDefaultExprCommand command = requirePayload(payload, ChangeConstraintDefaultExprCommand.class);
        yield targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case DELETE_CONSTRAINT -> {
        DeleteConstraintCommand command = requirePayload(payload, DeleteConstraintCommand.class);
        yield targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
      }
      case ADD_CONSTRAINT_COLUMN -> resolveStructuralOr(payload, () -> {
        AddConstraintColumnCommand command = requirePayload(payload, AddConstraintColumnCommand.class);
        return targetLookup.resolveByConstraintId(command.constraintId(), null);
      });
      case REMOVE_CONSTRAINT_COLUMN -> resolveStructuralOr(payload, () -> {
        RemoveConstraintColumnCommand command = requirePayload(payload, RemoveConstraintColumnCommand.class);
        return targetLookup.resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
      });
      case CHANGE_CONSTRAINT_COLUMN_POSITION -> {
        ChangeConstraintColumnPositionCommand command = requirePayload(payload,
            ChangeConstraintColumnPositionCommand.class);
        yield targetLookup.resolveByConstraintColumnId(command.constraintColumnId(), command.constraintColumnId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveIndexOperation(ErdOperationType operationType, Object payload) {
    return switch (operationType) {
      case CREATE_INDEX -> {
        CreateIndexCommand command = requirePayload(payload, CreateIndexCommand.class);
        yield targetLookup.resolveTableContext(command.tableId());
      }
      case CHANGE_INDEX_NAME -> resolveChangeIndexName(payload);
      case CHANGE_INDEX_TYPE -> resolveChangeIndexType(payload);
      case DELETE_INDEX -> {
        DeleteIndexCommand command = requirePayload(payload, DeleteIndexCommand.class);
        yield targetLookup.resolveByIndexId(command.indexId(), command.indexId());
      }
      case ADD_INDEX_COLUMN -> resolveStructuralOr(payload, () -> {
        AddIndexColumnCommand command = requirePayload(payload, AddIndexColumnCommand.class);
        return targetLookup.resolveByIndexId(command.indexId(), null);
      });
      case REMOVE_INDEX_COLUMN -> resolveStructuralOr(payload, () -> {
        RemoveIndexColumnCommand command = requirePayload(payload, RemoveIndexColumnCommand.class);
        return targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      });
      case CHANGE_INDEX_COLUMN_POSITION -> {
        ChangeIndexColumnPositionCommand command = requirePayload(payload,
            ChangeIndexColumnPositionCommand.class);
        yield targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      }
      case CHANGE_INDEX_COLUMN_SORT_DIRECTION -> {
        ChangeIndexColumnSortDirectionCommand command = requirePayload(payload,
            ChangeIndexColumnSortDirectionCommand.class);
        yield targetLookup.resolveByIndexColumnId(command.indexColumnId(), command.indexColumnId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveRelationshipOperation(
      ErdOperationType operationType,
      Object payload) {
    return switch (operationType) {
      case CREATE_RELATIONSHIP -> {
        CreateRelationshipCommand command = requirePayload(payload, CreateRelationshipCommand.class);
        yield targetLookup.resolveTableContext(command.fkTableId());
      }
      case CHANGE_RELATIONSHIP_NAME -> resolveChangeRelationshipName(payload);
      case CHANGE_RELATIONSHIP_KIND -> {
        ChangeRelationshipKindCommand command = requirePayload(payload, ChangeRelationshipKindCommand.class);
        yield targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case CHANGE_RELATIONSHIP_CARDINALITY -> resolveChangeRelationshipCardinality(payload);
      case CHANGE_RELATIONSHIP_EXTRA -> {
        ChangeRelationshipExtraCommand command = requirePayload(payload, ChangeRelationshipExtraCommand.class);
        yield targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case DELETE_RELATIONSHIP -> {
        DeleteRelationshipCommand command = requirePayload(payload, DeleteRelationshipCommand.class);
        yield targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
      }
      case ADD_RELATIONSHIP_COLUMN -> resolveStructuralOr(payload, () -> {
        AddRelationshipColumnCommand command = requirePayload(payload, AddRelationshipColumnCommand.class);
        return targetLookup.resolveByRelationshipId(command.relationshipId(), null);
      });
      case REMOVE_RELATIONSHIP_COLUMN -> resolveStructuralOr(payload, () -> {
        RemoveRelationshipColumnCommand command = requirePayload(payload, RemoveRelationshipColumnCommand.class);
        return targetLookup.resolveByRelationshipColumnId(
            command.relationshipColumnId(), command.relationshipColumnId());
      });
      case CHANGE_RELATIONSHIP_COLUMN_POSITION -> {
        ChangeRelationshipColumnPositionCommand command = requirePayload(payload,
            ChangeRelationshipColumnPositionCommand.class);
        yield targetLookup.resolveByRelationshipColumnId(
            command.relationshipColumnId(), command.relationshipColumnId());
      }
      default -> throw unsupportedTargetOperation(operationType);
    };
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeTableName(Object payload) {
    if (payload instanceof ChangeTableNameInverse inverse) {
      return targetLookup.resolveByTableId(inverse.tableId(), inverse.tableId());
    }
    ChangeTableNameCommand command = requirePayload(payload, ChangeTableNameCommand.class);
    return targetLookup.resolveByTableId(command.tableId(), command.tableId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnName(Object payload) {
    if (payload instanceof ChangeColumnNameInverse inverse) {
      return targetLookup.resolveByColumnId(inverse.columnId(), inverse.columnId());
    }
    ChangeColumnNameCommand command = requirePayload(payload, ChangeColumnNameCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeColumnType(Object payload) {
    if (payload instanceof ChangeColumnTypeInverse inverse) {
      return targetLookup.resolveByColumnId(inverse.columnId(), inverse.columnId());
    }
    ChangeColumnTypeCommand command = requirePayload(payload, ChangeColumnTypeCommand.class);
    return targetLookup.resolveByColumnId(command.columnId(), command.columnId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeConstraintName(Object payload) {
    if (payload instanceof ChangeConstraintNameInverse inverse) {
      return targetLookup.resolveByConstraintId(inverse.constraintId(), inverse.constraintId());
    }
    ChangeConstraintNameCommand command = requirePayload(payload, ChangeConstraintNameCommand.class);
    return targetLookup.resolveByConstraintId(command.constraintId(), command.constraintId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexName(Object payload) {
    if (payload instanceof ChangeIndexNameInverse inverse) {
      return targetLookup.resolveByIndexId(inverse.indexId(), inverse.indexId());
    }
    ChangeIndexNameCommand command = requirePayload(payload, ChangeIndexNameCommand.class);
    return targetLookup.resolveByIndexId(command.indexId(), command.indexId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeIndexType(Object payload) {
    if (payload instanceof ChangeIndexTypeInverse inverse) {
      return targetLookup.resolveByIndexId(inverse.indexId(), inverse.indexId());
    }
    ChangeIndexTypeCommand command = requirePayload(payload, ChangeIndexTypeCommand.class);
    return targetLookup.resolveByIndexId(command.indexId(), command.indexId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipName(Object payload) {
    if (payload instanceof ChangeRelationshipNameInverse inverse) {
      return targetLookup.resolveByRelationshipId(inverse.relationshipId(), inverse.relationshipId());
    }
    ChangeRelationshipNameCommand command = requirePayload(payload, ChangeRelationshipNameCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveChangeRelationshipCardinality(Object payload) {
    if (payload instanceof ChangeRelationshipCardinalityInverse inverse) {
      return targetLookup.resolveByRelationshipId(inverse.relationshipId(), inverse.relationshipId());
    }
    ChangeRelationshipCardinalityCommand command = requirePayload(payload,
        ChangeRelationshipCardinalityCommand.class);
    return targetLookup.resolveByRelationshipId(command.relationshipId(), command.relationshipId());
  }

  private Mono<ResolvedErdMutationTarget> resolveStructuralInverse(StructuralOperationInverse inverse) {
    return targetLookup.resolveBySchemaId(inverse.schemaId(), inverse.touchedEntityId());
  }

  private Mono<ResolvedErdMutationTarget> resolveStructuralOr(
      Object payload,
      Supplier<Mono<ResolvedErdMutationTarget>> fallback) {
    if (payload instanceof StructuralOperationInverse inverse) {
      return resolveStructuralInverse(inverse);
    }
    return fallback.get();
  }

  private static <T> T requirePayload(Object payload, Class<T> type) {
    if (!type.isInstance(payload)) {
      throw new IllegalArgumentException(
          "Unexpected payload type for %s: %s".formatted(type.getSimpleName(), describeType(payload)));
    }
    return type.cast(payload);
  }

  private static String describeType(Object value) {
    return value == null ? "null" : value.getClass().getName();
  }

  private static IllegalArgumentException unsupportedTargetOperation(ErdOperationType operationType) {
    return new IllegalArgumentException("Unsupported target operation: " + operationType);
  }

}
