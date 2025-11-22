package com.schemafy.core.project.controller.dto.response;

import java.time.LocalDateTime;

public record ProjectSummaryResponse(String id, String workspaceId, String name,
        String description, String myRole, Long memberCount,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
