package com.schemafy.domain.erd.table.application.port.in;

public record GetTableQuery(String tableId) {

  public GetTableQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }

}
