package com.schemafy.domain.erd.application.port.in;

public record ChangeTableMetaCommand(
    String tableId,
    String charset,
    String collation) {
}
