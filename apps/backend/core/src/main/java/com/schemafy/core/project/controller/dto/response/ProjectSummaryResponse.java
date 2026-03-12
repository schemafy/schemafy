package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.domain.project.application.port.in.ProjectSummary;
import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ProjectRole;

public record ProjectSummaryResponse(String id, String workspaceId, String name,
    String description, String myRole,
    Instant createdAt, Instant updatedAt) {

  public static ProjectSummaryResponse of(Project project, ProjectRole myRole) {
    return new ProjectSummaryResponse(
        project.getId(),
        project.getWorkspaceId(),
        project.getName(),
        project.getDescription(),
        myRole.name(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  public static ProjectSummaryResponse from(ProjectSummary detail) {
    return of(detail.project(), detail.role());
  }

}
