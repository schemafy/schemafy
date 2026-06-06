package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record CreateRelationshipInverse(
    String schemaId,
    String relationshipId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public CreateRelationshipInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return relationshipId;
  }

}
