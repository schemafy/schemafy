package com.schemafy.domain.erd;

public record Table(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation) {
}
