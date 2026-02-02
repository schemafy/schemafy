package com.schemafy.domain.erd.table.domain;

public record Table(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation) {
}
