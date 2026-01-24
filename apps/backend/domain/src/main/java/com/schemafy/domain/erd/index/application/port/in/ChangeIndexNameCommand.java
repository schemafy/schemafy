package com.schemafy.domain.erd.index.application.port.in;

public record ChangeIndexNameCommand(
    String indexId,
    String newName) {
}
