package com.schemafy.domain.erd.table.application.port.in;

public record GetTablesBySchemaIdQuery(String schemaId) {

  public GetTablesBySchemaIdQuery {
    if (schemaId == null || schemaId.isBlank()) {
      throw new IllegalArgumentException("schemaId must not be blank");
    }
  }

}
