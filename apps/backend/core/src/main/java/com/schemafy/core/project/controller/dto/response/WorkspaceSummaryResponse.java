package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;

public record WorkspaceSummaryResponse(String id, String name,
    String description, Instant createdAt, Instant updatedAt) {

  public static WorkspaceSummaryResponse of(Workspace workspace) {
    return new WorkspaceSummaryResponse(workspace.getId(),
        workspace.getName(), workspace.getDescription(),
        workspace.getCreatedAt(), workspace.getUpdatedAt());
  }

}
