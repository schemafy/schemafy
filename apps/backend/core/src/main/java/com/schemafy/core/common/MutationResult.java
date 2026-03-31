package com.schemafy.core.common;

import java.util.Collections;
import java.util.Set;

import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

public record MutationResult<T>(
    T result,
    Set<String> affectedTableIds,
    CommittedErdOperation operation) {

  public MutationResult {
    affectedTableIds = affectedTableIds == null
        ? Collections.emptySet()
        : Set.copyOf(affectedTableIds);
  }

  public static <T> MutationResult<T> empty(T result) {
    return new MutationResult<>(result, Collections.emptySet(), null);
  }

  public static <T> MutationResult<T> of(T result, String tableId) {
    if (tableId == null) {
      return new MutationResult<>(result, Collections.emptySet(), null);
    }
    return new MutationResult<>(result, Set.of(tableId), null);
  }

  public static <T> MutationResult<T> of(T result, Set<String> tableIds) {
    return new MutationResult<>(result, tableIds, null);
  }

  public MutationResult<T> withOperation(CommittedErdOperation operation) {
    return new MutationResult<>(result, affectedTableIds, operation);
  }

}
