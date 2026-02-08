package com.schemafy.core.erd.controller.dto.response;

import com.schemafy.domain.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.domain.erd.schema.domain.Schema;

public record SchemaResponse(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

  public static SchemaResponse from(CreateSchemaResult result) {
    return new SchemaResponse(
        result.id(),
        result.projectId(),
        result.dbVendorName(),
        result.name(),
        result.charset(),
        result.collation());
  }

  public static SchemaResponse from(Schema schema) {
    return new SchemaResponse(
        schema.id(),
        schema.projectId(),
        schema.dbVendorName(),
        schema.name(),
        schema.charset(),
        schema.collation());
  }

}
