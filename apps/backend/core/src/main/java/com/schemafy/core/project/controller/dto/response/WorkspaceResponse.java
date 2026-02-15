package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;

public record WorkspaceResponse(String id, String name, String description,
    Instant createdAt, Instant updatedAt,
    Long memberCount, Long projectCount, String currentUserRole) {

  public static WorkspaceResponse of(Workspace workspace,
      Long memberCount, Long projectCount, String currentUserRole) {
    return new WorkspaceResponse(workspace.getId(), workspace.getName(),
        workspace.getDescription(),
        workspace.getCreatedAt(), workspace.getUpdatedAt(),
        memberCount, projectCount, currentUserRole);
  }

}
