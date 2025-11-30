package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectSettings;

public record ProjectResponse(String id, String workspaceId, String ownerId,
        String name, String description, ProjectSettings settings,
        Instant createdAt, Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getWorkspaceId(),
                project.getOwnerId(), project.getName(),
                project.getDescription(), project.getSettingsAsVo(),
                project.getCreatedAt(), project.getUpdatedAt());
    }
}
