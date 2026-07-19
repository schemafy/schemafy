package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeColumnPositionInverse(
    String columnId,
    List<ReorderPosition> positions) implements InversePayload {

  public ChangeColumnPositionInverse {
    positions = positions == null ? List.of() : List.copyOf(positions);
  }

}
