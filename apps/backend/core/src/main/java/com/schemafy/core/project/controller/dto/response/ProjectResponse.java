package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectSettings;

public record ProjectResponse(String id, String workspaceId,
    String name, String description, ProjectSettings settings,
    Instant createdAt, Instant updatedAt,
    Long memberCount, String currentUserRole) {

  public static ProjectResponse of(Project project, Long memberCount,
      String currentUserRole) {
    return new ProjectResponse(project.getId(), project.getWorkspaceId(),
        project.getName(), project.getDescription(),
        project.getSettingsAsVo(),
        project.getCreatedAt(), project.getUpdatedAt(),
        memberCount, currentUserRole);
  }

}
