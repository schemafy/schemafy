package com.schemafy.core.workspace.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.workspace.repository.entity.Workspace;
import com.schemafy.core.workspace.repository.vo.WorkspaceSettings;

public record WorkspaceResponse(String id, String name, String description,
        String ownerId, WorkspaceSettings settings, Instant createdAt,
        Instant updatedAt) {

    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(workspace.getId(), workspace.getName(),
                workspace.getDescription(), workspace.getOwnerId(),
                workspace.getSettingsAsVo(), workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

}
