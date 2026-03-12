package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.Workspace;

public record WorkspaceDetail(
    Workspace workspace,
    Long projectCount,
    String currentUserRole) {
}
