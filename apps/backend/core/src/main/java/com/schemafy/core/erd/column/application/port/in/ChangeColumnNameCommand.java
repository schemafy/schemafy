package com.schemafy.core.erd.column.application.port.in;

public record ChangeColumnNameCommand(
    String columnId,
    String newName) {
}
