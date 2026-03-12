package com.schemafy.core.project.controller.dto.response;

import com.schemafy.domain.project.domain.Project;

public record ShareLinkAccessResponse(
    String projectId,
    String projectName,
    String description) {

  public static ShareLinkAccessResponse of(Project project) {
    return new ShareLinkAccessResponse(project.getId(), project.getName(),
        project.getDescription());
  }

}
