package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.column.application.port.in.CreateColumnResult;
import com.schemafy.core.erd.constraint.application.port.in.AddConstraintColumnResult;
import com.schemafy.core.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.core.erd.index.application.port.in.AddIndexColumnResult;
import com.schemafy.core.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.core.erd.operation.application.inverse.StructuralOperationInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.ErdTouchedEntity;
import com.schemafy.core.erd.relationship.application.port.in.AddRelationshipColumnResult;
import com.schemafy.core.erd.relationship.application.port.in.CreateRelationshipResult;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.core.erd.table.application.port.in.CreateTableResult;

@Component
class ErdMutationTargetFinalizer {

  <T> FinalizedErdMutationTarget finalizeTarget(
      ErdOperationType operationType,
      Object payload,
      ResolvedErdMutationTarget resolvedTarget,
      MutationResult<T> mutationResult) {
    String resolvedSchemaId = resolvedTarget.schemaId();
    String touchedEntityId = operationType.createsEntity()
        ? resolveCreatedEntityId(operationType, payload, resolvedTarget, mutationResult)
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

  private <T> String resolveCreatedEntityId(
      ErdOperationType operationType,
      Object payload,
      ResolvedErdMutationTarget resolvedTarget,
      MutationResult<T> mutationResult) {
    if (payload instanceof StructuralOperationInverse && resolvedTarget.touchedEntityId() != null) {
      return resolvedTarget.touchedEntityId();
    }

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

}
