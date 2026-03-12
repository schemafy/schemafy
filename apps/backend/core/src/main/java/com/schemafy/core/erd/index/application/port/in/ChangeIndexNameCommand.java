package com.schemafy.core.erd.index.application.port.in;

public record ChangeIndexNameCommand(
    String indexId,
    String newName) {
}
