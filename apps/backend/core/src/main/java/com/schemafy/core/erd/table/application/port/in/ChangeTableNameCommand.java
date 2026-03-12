package com.schemafy.core.erd.table.application.port.in;

public record ChangeTableNameCommand(
    String tableId,
    String newName) {
}
