package com.schemafy.domain.erd.application.port.in;

public record CreateSchemaResult(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation
) {

}
