package com.schemafy.core.erd.schema.application.port.in;

public record CreateSchemaResult(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {

}
