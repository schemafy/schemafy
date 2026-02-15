package com.schemafy.domain.erd.column.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetColumnQuery(String columnId) {

  public GetColumnQuery {
    if (columnId == null || columnId.isBlank()) {
      throw new InvalidValueException("columnId must not be blank");
    }
  }

}
