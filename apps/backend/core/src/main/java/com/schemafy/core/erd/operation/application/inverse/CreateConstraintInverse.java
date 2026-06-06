package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record CreateConstraintInverse(
    String schemaId,
    String constraintId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public CreateConstraintInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return constraintId;
  }

}
