package com.schemafy.domain.erd.table.application.port.in;

public record ChangeTableExtraCommand(
    String tableId,
    String extra) {
}
