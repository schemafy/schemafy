package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetSchemaQuery(String schemaId) {

  public GetSchemaQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new InvalidValueException("schemaId must not be blank");
    }
  }

}
