package com.schemafy.domain.erd.index.application.port.in;

public record ChangeIndexColumnPositionCommand(
    String indexColumnId,
    int seqNo) {
}
