package com.schemafy.core.common.type;

import java.util.Collection;
import java.util.List;

public record MutationResponse<T>(T data, List<String> affectedTableIds) {

  public static <T> MutationResponse<T> of(T data, Collection<String> affectedTableIds) {
    if (affectedTableIds == null || affectedTableIds.isEmpty()) {
      return new MutationResponse<>(data, List.of());
    }
    return new MutationResponse<>(data, List.copyOf(affectedTableIds));
  }

}
