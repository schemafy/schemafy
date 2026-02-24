package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.service.dto.ProjectDetail;

public record ProjectResponse(String id, String workspaceId,
    String name, String description,
    Instant createdAt, Instant updatedAt,
    String currentUserRole) {

  public static ProjectResponse from(ProjectDetail detail) {
    Project project = detail.project();
    return new ProjectResponse(project.getId(), project.getWorkspaceId(),
        project.getName(), project.getDescription(),
        project.getCreatedAt(), project.getUpdatedAt(),
        detail.currentUserRole());
  }

}
