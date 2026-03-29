package com.schemafy.core.common;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public MutationResult<T> merge(Set<String> additionalTableIds) {
    if (additionalTableIds == null || additionalTableIds.isEmpty()) {
      return this;
    }
    return new MutationResult<>(result,
        Stream.concat(affectedTableIds.stream(), additionalTableIds.stream())
            .collect(Collectors.toUnmodifiableSet()),
        operation);
  }

}
