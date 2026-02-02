package com.schemafy.domain.erd.table.application.port.in;

public record ChangeTableMetaCommand(
    String tableId,
    String charset,
    String collation) {
}
