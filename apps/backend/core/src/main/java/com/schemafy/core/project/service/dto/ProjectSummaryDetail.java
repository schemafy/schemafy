package com.schemafy.core.project.service.dto;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectRole;

public record ProjectSummaryDetail(Project project, ProjectRole role) {

}
