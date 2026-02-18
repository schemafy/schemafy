package com.schemafy.core.project.service.dto;

import com.schemafy.core.project.repository.entity.Workspace;

public record WorkspaceDetail(Workspace workspace, Long memberCount,
    Long projectCount, String currentUserRole) {

}
