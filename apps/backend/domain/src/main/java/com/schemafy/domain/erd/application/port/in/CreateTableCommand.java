package com.schemafy.domain.erd.application.port.in;

public record CreateTableCommand(
    String schemaId,
    String name,
    String charset,
    String collation) {
}
