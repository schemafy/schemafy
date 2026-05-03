package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record AddConstraintColumnInverse(
    String schemaId,
    String constraintColumnId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public AddConstraintColumnInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return constraintColumnId;
  }
}
