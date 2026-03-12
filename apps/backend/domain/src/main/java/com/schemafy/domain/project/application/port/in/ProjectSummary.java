package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.Project;
import com.schemafy.domain.project.domain.ProjectRole;

public record ProjectSummary(Project project, ProjectRole role) {
}
