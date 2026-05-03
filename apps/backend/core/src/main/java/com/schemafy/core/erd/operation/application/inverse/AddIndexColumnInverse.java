package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record AddIndexColumnInverse(
    String schemaId,
    String indexColumnId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public AddIndexColumnInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return indexColumnId;
  }
}
