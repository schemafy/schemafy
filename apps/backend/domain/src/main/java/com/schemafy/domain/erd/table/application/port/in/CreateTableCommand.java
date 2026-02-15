package com.schemafy.domain.erd.table.application.port.in;

public record CreateTableCommand(
    String schemaId,
    String name,
    String charset,
    String collation) {
}
