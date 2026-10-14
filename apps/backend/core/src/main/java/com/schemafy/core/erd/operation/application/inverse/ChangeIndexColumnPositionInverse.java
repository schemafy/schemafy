package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeIndexColumnPositionInverse(
    String indexColumnId,
    List<ReorderPosition> positions) implements InversePayload {

  public ChangeIndexColumnPositionInverse {
    positions = positions == null ? List.of() : List.copyOf(positions);
  }

}
