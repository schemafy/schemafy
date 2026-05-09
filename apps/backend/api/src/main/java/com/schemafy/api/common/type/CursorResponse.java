package com.schemafy.api.common.type;

import java.util.List;
import java.util.function.Function;

public record CursorResponse<T>(
    List<T> content,
    int size,
    boolean hasNext,
    String nextCursorId) {

  public <R> CursorResponse<R> map(Function<T, R> mapper) {
    List<R> mappedContent = content.stream().map(mapper).toList();
    return new CursorResponse<>(mappedContent, size, hasNext, nextCursorId);
  }

  public static <T> CursorResponse<T> of(
      List<T> content,
      int size,
      boolean hasNext,
      String nextCursorId) {
    return new CursorResponse<>(content, size, hasNext, nextCursorId);
  }

  public static <T> CursorResponse<T> empty(int size) {
    return new CursorResponse<>(List.of(), size, false, null);
  }

}
