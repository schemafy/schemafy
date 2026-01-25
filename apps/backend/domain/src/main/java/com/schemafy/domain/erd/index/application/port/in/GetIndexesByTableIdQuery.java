package com.schemafy.domain.erd.index.application.port.in;

public record GetIndexesByTableIdQuery(String tableId) {

  public GetIndexesByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }

}
