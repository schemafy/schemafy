package com.schemafy.domain.erd.schema.application.port.in;

import com.schemafy.domain.common.exception.InvalidValueException;

public record GetSchemasByProjectIdQuery(String projectId) {

  public GetSchemasByProjectIdQuery {
    if (projectId == null || projectId.isBlank()) {
      throw new InvalidValueException("projectId must not be blank");
    }
  }

}
