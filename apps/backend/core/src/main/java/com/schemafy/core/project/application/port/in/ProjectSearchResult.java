package com.schemafy.core.project.application.port.in;

import java.time.Instant;

public record ProjectSearchResult(
    String id,
    String workspaceId,
    Integer dbVendorId,
    String name,
    String description,
    String requesterRole,
    Instant createdAt,
    Instant updatedAt) {
}
