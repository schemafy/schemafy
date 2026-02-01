package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetColumnsByTableIdQuery(String tableId) {

  public GetColumnsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new InvalidValueException("tableId must not be blank");
    }
  }

}
