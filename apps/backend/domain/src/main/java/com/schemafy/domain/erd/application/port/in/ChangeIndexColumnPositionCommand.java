package com.schemafy.domain.erd.application.port.in;

public record ChangeIndexColumnPositionCommand(
    String indexColumnId,
    int seqNo) {
}
