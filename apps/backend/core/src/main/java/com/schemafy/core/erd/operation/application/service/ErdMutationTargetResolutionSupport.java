package com.schemafy.core.erd.operation.application.service;

import java.util.function.Supplier;

import com.schemafy.core.erd.operation.application.inverse.StructuralOperationInverse;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

final class ErdMutationTargetResolutionSupport {

  private ErdMutationTargetResolutionSupport() {}

  static <T> T requirePayload(Object payload, Class<T> type) {
    if (!type.isInstance(payload)) {
      throw new IllegalArgumentException(
          "Unexpected payload type for %s: %s".formatted(type.getSimpleName(), describeType(payload)));
    }
    return type.cast(payload);
  }

  static Mono<ResolvedErdMutationTarget> resolveStructuralOr(
      Object payload,
      ErdMutationTargetLookup targetLookup,
      Supplier<Mono<ResolvedErdMutationTarget>> fallback) {
    if (payload instanceof StructuralOperationInverse inverse) {
      return targetLookup.resolveBySchemaId(inverse.schemaId(), inverse.touchedEntityId());
    }
    return fallback.get();
  }

  static IllegalArgumentException unsupportedTargetOperation(ErdOperationType operationType) {
    return new IllegalArgumentException("Unsupported target operation: " + operationType);
  }

  private static String describeType(Object value) {
    return value == null ? "null" : value.getClass().getName();
  }

}
