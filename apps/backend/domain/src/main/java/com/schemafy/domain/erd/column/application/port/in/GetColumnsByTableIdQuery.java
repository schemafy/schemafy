package com.schemafy.domain.erd.column.application.port.in;

public record GetColumnsByTableIdQuery(String tableId) {

  public GetColumnsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }

}
