package com.schemafy.domain.erd.index.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetIndexColumnQuery(String indexColumnId) {

  public GetIndexColumnQuery {
    if (indexColumnId == null || indexColumnId.isBlank()) {
      throw new InvalidValueException("indexColumnId must not be blank");
    }
  }

}
