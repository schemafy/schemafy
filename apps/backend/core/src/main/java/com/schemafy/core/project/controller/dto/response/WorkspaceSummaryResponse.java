package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;

public record WorkspaceSummaryResponse(String id, String name,
        String description, String ownerId, Instant createdAt,
        Instant updatedAt, Long memberCount) {

    public static WorkspaceSummaryResponse of(Workspace workspace,
            Long memberCount) {
        return new WorkspaceSummaryResponse(workspace.getId(),
                workspace.getName(), workspace.getDescription(),
                workspace.getOwnerId(), workspace.getCreatedAt(),
                workspace.getUpdatedAt(), memberCount);
    }

}
