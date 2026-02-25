package com.schemafy.domain.erd.table.application.port.in;

public record CreateTableResult(
    String tableId,
    String name,
    String charset,
    String collation,
    String extra) {

}
