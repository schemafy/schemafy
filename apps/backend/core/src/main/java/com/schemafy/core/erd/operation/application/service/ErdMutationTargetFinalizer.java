package com.schemafy.core.erd.operation.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;

@Component
class ErdMutationTargetFinalizer {

  <T> FinalizedErdMutationTarget finalizeTarget(
      ErdOperationType operationType,
      ResolvedErdMutationTarget resolvedTarget,
      MutationResult<T> mutationResult) {
    String resolvedSchemaId = resolvedTarget.schemaId();

    if (resolvedSchemaId == null && operationType == ErdOperationType.CREATE_SCHEMA) {
      resolvedSchemaId = resolveCreatedSchemaId(mutationResult);
    }

    return new FinalizedErdMutationTarget(
        resolvedTarget.projectId(),
        resolvedSchemaId);
  }

  private <T> String resolveCreatedSchemaId(MutationResult<T> mutationResult) {
    return requireResult(mutationResult.result(), CreateSchemaResult.class).id();
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
