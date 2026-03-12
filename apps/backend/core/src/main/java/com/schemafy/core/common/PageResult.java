package com.schemafy.core.common;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {

  public <R> PageResult<R> map(Function<T, R> mapper) {
    List<R> mappedContent = content.stream().map(mapper).toList();
    return new PageResult<>(mappedContent, page, size, totalElements,
        totalPages);
  }

  public static <T> PageResult<T> of(
      List<T> content,
      int page,
      int size,
      long totalElements) {
    int totalPages = size <= 0 ? 0
        : (int) Math.ceil((double) totalElements / size);
    return new PageResult<>(content, page, size, totalElements,
        totalPages);
  }

  public static <T> PageResult<T> empty(int page, int size) {
    return new PageResult<>(List.of(), page, size, 0, 0);
  }

}
