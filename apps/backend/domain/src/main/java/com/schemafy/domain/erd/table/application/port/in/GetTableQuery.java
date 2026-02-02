package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetTableQuery(String tableId) {

  public GetTableQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new InvalidValueException("tableId must not be blank");
    }
  }

}
