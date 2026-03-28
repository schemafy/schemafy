package com.schemafy.core.erd.operation.domain;

import java.time.Instant;

public record SchemaCollaborationState(
    String schemaId,
    String projectId,
    long currentRevision,
    Instant createdAt,
    Instant updatedAt) {
}
