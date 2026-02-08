package com.schemafy.domain.erd.table.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetTablesBySchemaIdQuery(String schemaId) {

  public GetTablesBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new InvalidValueException("schemaId must not be blank");
    }
  }

}
