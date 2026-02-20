package com.schemafy.core.project.controller.dto.response;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectSettings;

public record ShareLinkAccessResponse(
    String projectId,
    String projectName,
    String description,
    ProjectSettings settings) {

  public static ShareLinkAccessResponse of(Project project) {
    return new ShareLinkAccessResponse(project.getId(), project.getName(),
        project.getDescription(), project.getSettingsAsVo());
  }

}
