package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record DeleteRelationshipInverse(
    String schemaId,
    String relationshipId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public DeleteRelationshipInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return relationshipId;
  }

}
