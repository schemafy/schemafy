package com.schemafy.core.project.service.dto;

import com.schemafy.core.project.repository.entity.Project;

public record ProjectDetail(Project project, Long memberCount,
    String currentUserRole) {

}
