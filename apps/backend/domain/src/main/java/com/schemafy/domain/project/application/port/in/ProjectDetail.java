package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.Project;

public record ProjectDetail(Project project, String currentUserRole) {
}
