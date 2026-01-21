package com.schemafy.domain.erd.application.port.in;

public record CreateTableResult(
    String tableId,
    String name,
    String charset,
    String collation) {

}
