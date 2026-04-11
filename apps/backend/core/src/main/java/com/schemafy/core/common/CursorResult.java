package com.schemafy.core.common;

import java.util.List;
import java.util.function.Function;

public record CursorResult<T>(
    List<T> content,
    int size,
    boolean hasNext,
    String nextCursorId) {

  public <R> CursorResult<R> map(Function<T, R> mapper) {
    List<R> mappedContent = content.stream().map(mapper).toList();
    return new CursorResult<>(mappedContent, size, hasNext, nextCursorId);
  }

  public static <T> CursorResult<T> of(
      List<T> content,
      int size,
      boolean hasNext,
      String nextCursorId) {
    return new CursorResult<>(content, size, hasNext, nextCursorId);
  }

  public static <T> CursorResult<T> fromFetchedPage(
      List<T> fetchedContents,
      int size,
      Function<T, String> cursorExtractor) {
    boolean hasNext = fetchedContents.size() > size;
    List<T> content = hasNext ? fetchedContents.subList(0, size)
        : fetchedContents;
    String nextCursorId = hasNext && !content.isEmpty()
        ? cursorExtractor.apply(content.getLast())
        : null;
    return CursorResult.of(content, size, hasNext, nextCursorId);
  }

  public static <T> CursorResult<T> empty(int size) {
    return new CursorResult<>(List.of(), size, false, null);
  }

}
