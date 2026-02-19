package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.service.dto.ProjectSummaryDetail;

public record ProjectSummaryResponse(String id, String workspaceId, String name,
    String description, String myRole,
    Instant createdAt, Instant updatedAt) {

  public static ProjectSummaryResponse of(Project project, ProjectRole myRole) {
    return new ProjectSummaryResponse(
        project.getId(),
        project.getWorkspaceId(),
        project.getName(),
        project.getDescription(),
        myRole.getValue(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  public static ProjectSummaryResponse from(ProjectSummaryDetail detail) {
    return of(detail.project(), detail.role());
  }

}
