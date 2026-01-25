package com.schemafy.domain.erd.constraint.application.port.in;

public record GetConstraintsByTableIdQuery(String tableId) {

  public GetConstraintsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }

}
