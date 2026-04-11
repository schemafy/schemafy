package com.schemafy.core.erd.operation.domain;

public record ErdTouchedEntity(
    ErdTouchedEntityType entityType,
    String entityId) {
}
