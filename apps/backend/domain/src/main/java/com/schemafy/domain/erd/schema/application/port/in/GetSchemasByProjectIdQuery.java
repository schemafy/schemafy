package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;

public record GetSchemasByProjectIdQuery(String projectId) {

  public GetSchemasByProjectIdQuery {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "projectId must not be blank");
    }
  }

}
