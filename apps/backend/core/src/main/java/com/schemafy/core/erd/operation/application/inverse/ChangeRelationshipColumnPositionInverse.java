package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record ChangeRelationshipColumnPositionInverse(
    String relationshipColumnId,
    List<ReorderPosition> positions) implements InversePayload {

  public ChangeRelationshipColumnPositionInverse {
    positions = positions == null ? List.of() : List.copyOf(positions);
  }

}
