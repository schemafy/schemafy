package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.repository.vo.WorkspaceSettings;

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
