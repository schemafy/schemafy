package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record CreateIndexInverse(
    String schemaId,
    String indexId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public CreateIndexInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return indexId;
  }

}
