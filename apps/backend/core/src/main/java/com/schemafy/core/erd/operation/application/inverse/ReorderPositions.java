package com.schemafy.core.erd.operation.application.inverse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class ReorderPositions {

  private ReorderPositions() {}

  public static <T> List<ReorderPosition> capture(
      List<T> entities,
      Function<T, String> idExtractor,
      ToIntFunction<T> seqNoExtractor) {
    return entities.stream()
        .map(entity -> new ReorderPosition(
            idExtractor.apply(entity),
            seqNoExtractor.applyAsInt(entity)))
        .toList();
  }

  public static <T> List<T> restore(
      List<T> currentEntities,
      Function<T, String> idExtractor,
      List<ReorderPosition> snapshot,
      BiFunction<T, Integer, T> withSeqNo) {
    Map<String, Integer> positionsById = indexForRestore(
        currentEntities,
        idExtractor,
        snapshot);
    return currentEntities.stream()
        .map(entity -> withSeqNo.apply(
            entity,
            positionsById.get(idExtractor.apply(entity))))
        .toList();
  }

  private static <T> Map<String, Integer> indexForRestore(
      List<T> currentEntities,
      Function<T, String> idExtractor,
      List<ReorderPosition> snapshot) {
    if (snapshot.size() != currentEntities.size()) {
      throw snapshotMismatch();
    }

    Map<String, Integer> positionsById = new HashMap<>(snapshot.size());
    for (ReorderPosition position : snapshot) {
      if (positionsById.put(position.entityId(), position.seqNo()) != null) {
        throw snapshotMismatch();
      }
    }

    for (T entity : currentEntities) {
      if (!positionsById.containsKey(idExtractor.apply(entity))) {
        throw snapshotMismatch();
      }
    }
    return Map.copyOf(positionsById);
  }

  private static IllegalStateException snapshotMismatch() {
    return new IllegalStateException(
        "Reorder snapshot does not match current entities");
  }

}
