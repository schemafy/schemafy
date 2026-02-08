package com.schemafy.domain.erd.column.application.port.in;

public record CreateColumnCommand(
    String tableId,
    String name,
    String dataType,
    Integer length,
    Integer precision,
    Integer scale,
    boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
