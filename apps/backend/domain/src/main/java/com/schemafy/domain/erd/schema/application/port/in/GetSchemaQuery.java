package com.schemafy.domain.erd.schema.application.port.in;

public record GetSchemaQuery(String schemaId) {

  public GetSchemaQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new IllegalArgumentException("schemaId must not be blank");
    }
  }

}
