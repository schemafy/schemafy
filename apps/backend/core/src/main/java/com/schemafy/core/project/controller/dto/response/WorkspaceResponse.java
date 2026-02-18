package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Workspace;
import com.schemafy.core.project.service.dto.WorkspaceDetail;

public record WorkspaceResponse(String id, String name, String description,
    Instant createdAt, Instant updatedAt,
    Long memberCount, Long projectCount, String currentUserRole) {

  public static WorkspaceResponse from(WorkspaceDetail detail) {
    Workspace workspace = detail.workspace();
    return new WorkspaceResponse(workspace.getId(), workspace.getName(),
        workspace.getDescription(),
        workspace.getCreatedAt(), workspace.getUpdatedAt(),
        detail.memberCount(), detail.projectCount(),
        detail.currentUserRole());
  }

}
