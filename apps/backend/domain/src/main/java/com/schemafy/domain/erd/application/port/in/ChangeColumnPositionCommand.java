package com.schemafy.domain.erd.application.port.in;

public record ChangeColumnPositionCommand(
    String columnId,
    int seqNo) {
}
