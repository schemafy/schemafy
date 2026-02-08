package com.schemafy.domain.erd.table.application.port.in;

public record ChangeTableNameCommand(
    String tableId,
    String newName) {
}
