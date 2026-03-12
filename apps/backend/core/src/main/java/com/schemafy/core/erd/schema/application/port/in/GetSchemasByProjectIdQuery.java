package com.schemafy.core.erd.schema.application.port.in;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;

public record GetSchemasByProjectIdQuery(String projectId) {

  public GetSchemasByProjectIdQuery {
    if (projectId == null || projectId.isBlank()) {
      throw new DomainException(SchemaErrorCode.INVALID_VALUE, "projectId must not be blank");
    }
  }

}
