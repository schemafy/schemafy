package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record CreateTableInverse(
    String schemaId,
    String tableId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public CreateTableInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return tableId;
  }

}
