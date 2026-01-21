package com.schemafy.domain.erd.application.port.in;

public record ChangeTableExtraCommand(
    String tableId,
    String extra) {
}
