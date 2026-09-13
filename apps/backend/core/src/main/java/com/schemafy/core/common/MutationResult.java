package com.schemafy.core.common;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.schemafy.core.erd.operation.application.inverse.InversePayload;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

public record MutationResult<T>(
    T result,
    Set<String> affectedTableIds,
    CommittedErdOperation operation,
    InversePayload inversePayload,
    boolean noOp) {

  public MutationResult {
    affectedTableIds = affectedTableIds == null
        ? Collections.emptySet()
        : Set.copyOf(affectedTableIds);
  }

  public static <T> MutationResult<T> empty(T result) {
    return new MutationResult<>(result, Collections.emptySet(), null, null, false);
  }

  public static <T> MutationResult<T> of(T result, String tableId) {
    if (tableId == null) {
      return new MutationResult<>(result, Collections.emptySet(), null, null, false);
    }
    return new MutationResult<>(result, Set.of(tableId), null, null, false);
  }

  public static <T> MutationResult<T> of(T result, Set<String> tableIds) {
    return new MutationResult<>(result, tableIds, null, null, false);
  }

  public static <T> MutationResult<T> noop(T result) {
    return new MutationResult<>(result, Collections.emptySet(), null, null, true);
  }

  public static <T> MutationResult<T> noop(T result, String tableId) {
    if (tableId == null) {
      return noop(result);
    }
    return new MutationResult<>(result, Set.of(tableId), null, null, true);
  }

  public static <T> MutationResult<T> noop(T result, Set<String> tableIds) {
    return new MutationResult<>(result, tableIds, null, null, true);
  }

  public MutationResult<T> withOperation(CommittedErdOperation operation) {
    return new MutationResult<>(result, affectedTableIds, operation, inversePayload, false);
  }

  public MutationResult<T> withInverse(InversePayload inversePayload) {
    return new MutationResult<>(result, affectedTableIds, operation, inversePayload, noOp);
  }

  public List<String> sortedAffectedTableIds() {
    return affectedTableIds.stream()
        .sorted()
        .toList();
  }

}
