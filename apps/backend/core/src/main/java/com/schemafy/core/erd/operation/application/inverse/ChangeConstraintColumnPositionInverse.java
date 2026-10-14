package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeConstraintColumnPositionInverse(
    String constraintColumnId,
    List<ReorderPosition> positions) implements InversePayload {

  public ChangeConstraintColumnPositionInverse {
    positions = positions == null ? List.of() : List.copyOf(positions);
  }

}
