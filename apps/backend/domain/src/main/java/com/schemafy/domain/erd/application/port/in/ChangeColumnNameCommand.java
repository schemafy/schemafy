package com.schemafy.domain.erd.application.port.in;

public record ChangeColumnNameCommand(
    String columnId,
    String newName) {
}
