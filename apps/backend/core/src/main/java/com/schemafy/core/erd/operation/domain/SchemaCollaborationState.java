package com.schemafy.core.erd.operation.domain;

import java.time.Instant;

public record SchemaCollaborationState(
    String schemaId,
    String projectId,
    long currentRevision,
    Instant createdAt,
    Instant updatedAt,
    Long version) {

  public SchemaCollaborationState incremented() {
    return new SchemaCollaborationState(
        schemaId,
        projectId,
        currentRevision + 1,
        createdAt,
        updatedAt,
        version);
  }

}
