package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record CreateColumnInverse(
    String schemaId,
    String columnId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public CreateColumnInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return columnId;
  }

}
