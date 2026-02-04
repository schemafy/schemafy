package com.schemafy.domain.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record MutationResult<T>(
    T result,
    Set<String> affectedTableIds) {

  public MutationResult {
    affectedTableIds = affectedTableIds == null
        ? Collections.emptySet()
        : Set.copyOf(affectedTableIds);
  }

  public static <T> MutationResult<T> empty(T result) {
    return new MutationResult<>(result, Collections.emptySet());
  }

  public static <T> MutationResult<T> of(T result, String tableId) {
    if (tableId == null) {
      return new MutationResult<>(result, Collections.emptySet());
    }
    return new MutationResult<>(result, Set.of(tableId));
  }

  public static <T> MutationResult<T> of(T result, Set<String> tableIds) {
    return new MutationResult<>(result, tableIds);
  }

  public MutationResult<T> merge(Set<String> additionalTableIds) {
    if (additionalTableIds == null || additionalTableIds.isEmpty()) {
      return this;
    }
    Set<String> merged = new HashSet<>(affectedTableIds);
    merged.addAll(additionalTableIds);
    return new MutationResult<>(result, merged);
  }

}
