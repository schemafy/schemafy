package com.schemafy.core.workspace.controller.dto.response;

import java.time.Instant;

public record WorkspaceSummaryResponse(String id, String name,
        String description, String ownerId, String role, Long memberCount,
        Instant createdAt, Instant updatedAt) {
}
