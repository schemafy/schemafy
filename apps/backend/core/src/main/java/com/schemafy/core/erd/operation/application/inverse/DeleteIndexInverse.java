package com.schemafy.core.erd.operation.application.inverse;

import java.util.List;

public record DeleteIndexInverse(
    String schemaId,
    String indexId,
    StructuralSnapshot beforeSnapshot,
    StructuralSnapshot afterSnapshot,
    List<String> affectedTableIds) implements InversePayload, StructuralOperationInverse {

  public DeleteIndexInverse {
    affectedTableIds = List.copyOf(affectedTableIds == null ? List.of() : affectedTableIds);
  }

  @Override
  public String touchedEntityId() {
    return indexId;
  }

}
