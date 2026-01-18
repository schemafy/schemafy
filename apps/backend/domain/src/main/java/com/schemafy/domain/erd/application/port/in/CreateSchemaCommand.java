package com.schemafy.domain.erd.application.port.in;

public record CreateSchemaCommand(
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

}
