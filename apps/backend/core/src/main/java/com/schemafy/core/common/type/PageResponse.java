package com.schemafy.core.common.type;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> content, int page, int size,
    long totalElements, int totalPages) {

  public <R> PageResponse<R> map(Function<T, R> mapper) {
    List<R> mappedContent = content.stream().map(mapper).toList();
    return new PageResponse<>(mappedContent, page, size, totalElements,
        totalPages);
  }

  public static <T> PageResponse<T> of(List<T> content, int page, int size,
      long totalElements) {
    int totalPages = (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(content, page, size, totalElements,
        totalPages);
  }

  public static <T> PageResponse<T> empty(int page, int size) {
    return new PageResponse<>(List.of(), page, size, 0, 0);
  }

}
