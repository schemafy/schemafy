package com.schemafy.domain.erd.column.application.port.in;

public record ChangeColumnMetaCommand(
    String columnId,
    Boolean autoIncrement,
    String charset,
    String collation,
    String comment) {
}
