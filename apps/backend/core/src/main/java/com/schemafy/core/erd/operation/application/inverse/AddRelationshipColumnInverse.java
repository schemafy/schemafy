package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record AddRelationshipColumnInverse(
    String schemaId,
    String relationshipColumnId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public AddRelationshipColumnInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return relationshipColumnId;
  }

}
