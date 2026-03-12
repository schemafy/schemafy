package com.schemafy.core.erd.schema.application.port.in;

public record CreateSchemaCommand(
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

}
