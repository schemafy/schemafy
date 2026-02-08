package com.schemafy.domain.erd.schema.application.port.in;

public record CreateSchemaCommand(
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

}
