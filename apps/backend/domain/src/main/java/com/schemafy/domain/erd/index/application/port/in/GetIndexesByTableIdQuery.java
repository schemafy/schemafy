package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetIndexesByTableIdQuery(String tableId) {

  public GetIndexesByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new InvalidValueException("tableId must not be blank");
    }
  }

}
