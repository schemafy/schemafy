package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectRole;

public record ProjectSummary(Project project, ProjectRole role) {
}
