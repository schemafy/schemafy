package com.schemafy.domain.erd.domain;

public record Schema(
    String id,
    String projectId,
    String dbVendorName,
    String name,
    String charset,
    String collation) {
}
