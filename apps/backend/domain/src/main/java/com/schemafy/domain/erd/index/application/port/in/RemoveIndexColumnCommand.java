package com.schemafy.domain.erd.index.application.port.in;

public record RemoveIndexColumnCommand(
    String indexId,
    String indexColumnId) {
}
