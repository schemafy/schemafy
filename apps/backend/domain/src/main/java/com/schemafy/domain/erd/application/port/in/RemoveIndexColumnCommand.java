package com.schemafy.domain.erd.application.port.in;

public record RemoveIndexColumnCommand(
    String indexId,
    String indexColumnId) {
}
