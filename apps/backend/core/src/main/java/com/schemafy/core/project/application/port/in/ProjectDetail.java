package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.Project;

public record ProjectDetail(Project project, String currentUserRole) {
}
