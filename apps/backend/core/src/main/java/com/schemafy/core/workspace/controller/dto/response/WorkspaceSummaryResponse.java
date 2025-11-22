package com.schemafy.core.workspace.controller.dto.response;

import java.time.LocalDateTime;

public record WorkspaceSummaryResponse(String id, String name,
        String description, String ownerId, String role, Long memberCount,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
