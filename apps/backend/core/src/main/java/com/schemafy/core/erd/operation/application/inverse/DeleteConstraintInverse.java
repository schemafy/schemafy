package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record DeleteConstraintInverse(
    String schemaId,
    String constraintId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public DeleteConstraintInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return constraintId;
  }

}
