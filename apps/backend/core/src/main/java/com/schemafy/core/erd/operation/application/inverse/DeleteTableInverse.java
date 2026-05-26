package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record DeleteTableInverse(
    String schemaId,
    String tableId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public DeleteTableInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return tableId;
  }

}
