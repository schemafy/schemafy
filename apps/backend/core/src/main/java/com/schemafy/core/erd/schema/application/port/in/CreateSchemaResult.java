package com.schemafy.core.erd.schema.application.port.in;

public record CreateSchemaResult(
    String id,
    String projectId,
    String name,
    String charset,
    String collation) {

}
