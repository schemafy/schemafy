package com.schemafy.api.erd.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaResult;
import com.schemafy.core.erd.schema.domain.Schema;

public record SchemaResponse(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long currentRevision) {

  public static SchemaResponse from(CreateSchemaResult result) {
    return new SchemaResponse(
        result.id(),
        result.projectId(),
        result.dbVendorName(),
        result.name(),
        result.charset(),
        result.collation(),
        null);
  }

  public static SchemaResponse from(Schema schema) {
    return new SchemaResponse(
        schema.id(),
        schema.projectId(),
        schema.dbVendorName(),
        schema.name(),
        schema.charset(),
        schema.collation(),
        null);
  }

  public static SchemaResponse from(Schema schema, long currentRevision) {
    return new SchemaResponse(
        schema.id(),
        schema.projectId(),
        schema.dbVendorName(),
        schema.name(),
        schema.charset(),
        schema.collation(),
        currentRevision);
  }

}
