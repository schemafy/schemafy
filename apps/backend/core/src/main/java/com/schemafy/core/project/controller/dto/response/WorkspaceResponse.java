package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;

public record WorkspaceResponse(String id, String name, String description,
    String ownerId, Instant createdAt, Instant updatedAt) {

  public static WorkspaceResponse from(Workspace workspace) {
    return new WorkspaceResponse(workspace.getId(), workspace.getName(),
        workspace.getDescription(), workspace.getOwnerId(),
        workspace.getCreatedAt(), workspace.getUpdatedAt());
  }

}
