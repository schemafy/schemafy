package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.Workspace;

public record WorkspaceDetail(
    Workspace workspace,
    Long projectCount,
    String currentUserRole) {
}
