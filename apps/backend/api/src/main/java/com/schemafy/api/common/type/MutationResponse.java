package com.schemafy.api.common.type;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MutationResponse<T>(
    T data,
    List<String> affectedTableIds,
    CommittedErdOperation operation) {

  public static <T> MutationResponse<T> of(T data, Collection<String> affectedTableIds) {
    return of(data, affectedTableIds, null);
  }

  public static <T> MutationResponse<T> of(T data,
      Collection<String> affectedTableIds,
      CommittedErdOperation operation) {
    if (affectedTableIds == null || affectedTableIds.isEmpty()) {
      return new MutationResponse<>(data, List.of(), operation);
    }
    return new MutationResponse<>(data, List.copyOf(affectedTableIds),
        operation);
  }

}
