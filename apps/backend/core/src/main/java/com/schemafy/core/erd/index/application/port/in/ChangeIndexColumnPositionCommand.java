package com.schemafy.core.erd.index.application.port.in;

public record ChangeIndexColumnPositionCommand(
    String indexColumnId,
    int seqNo) {
}
