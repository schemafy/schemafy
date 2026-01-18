package com.schemafy.domain.erd.domain;

public record Table(
    String id,
    String schemaId,
    String name,
    String charset,
    String collation) {
}
