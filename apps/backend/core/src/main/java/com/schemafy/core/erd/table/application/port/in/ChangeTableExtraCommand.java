package com.schemafy.core.erd.table.application.port.in;

public record ChangeTableExtraCommand(
    String tableId,
    String extra) {
}
