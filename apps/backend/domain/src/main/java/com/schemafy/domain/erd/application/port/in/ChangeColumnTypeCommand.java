package com.schemafy.domain.erd.application.port.in;

public record ChangeColumnTypeCommand(
    String columnId,
    String dataType,
    Integer length,
    Integer precision,
    Integer scale) {
}
