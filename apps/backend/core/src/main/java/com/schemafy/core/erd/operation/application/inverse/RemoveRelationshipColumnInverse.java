package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record RemoveRelationshipColumnInverse(
    String schemaId,
    String relationshipColumnId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public RemoveRelationshipColumnInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return relationshipColumnId;
  }
}
