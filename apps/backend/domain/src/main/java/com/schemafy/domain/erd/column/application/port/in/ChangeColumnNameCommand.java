package com.schemafy.domain.erd.column.application.port.in;

public record ChangeColumnNameCommand(
    String columnId,
    String newName) {
}
