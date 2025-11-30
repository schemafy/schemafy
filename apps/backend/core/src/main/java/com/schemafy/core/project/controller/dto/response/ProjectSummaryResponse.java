package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

public record ProjectSummaryResponse(String id, String workspaceId, String name,
        String description, String myRole, Long memberCount,
        Instant createdAt, Instant updatedAt) {
}
