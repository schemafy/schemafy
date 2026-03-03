package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;

public record GetSchemaQuery(String schemaId) {

  public GetSchemaQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "schemaId must not be blank");
    }
  }

}
