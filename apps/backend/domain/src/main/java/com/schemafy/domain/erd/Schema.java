package com.schemafy.domain.erd;

public record Schema(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {
}
