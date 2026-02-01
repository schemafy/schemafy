package com.schemafy.domain.erd.constraint.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetConstraintsByTableIdQuery(String tableId) {

  public GetConstraintsByTableIdQuery {
    if (tableId == null || tableId.isBlank()) {
      throw new InvalidValueException("tableId must not be blank");
    }
  }

}
