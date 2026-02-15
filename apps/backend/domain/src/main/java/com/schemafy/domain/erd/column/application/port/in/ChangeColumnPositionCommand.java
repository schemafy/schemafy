package com.schemafy.domain.erd.column.application.port.in;

public record ChangeColumnPositionCommand(
    String columnId,
    int seqNo) {
}
