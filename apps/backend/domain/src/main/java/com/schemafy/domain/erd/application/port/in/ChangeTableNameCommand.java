package com.schemafy.domain.erd.application.port.in;

public record ChangeTableNameCommand(
    String schemaId,
    String tableId,
    String newName) {
}
