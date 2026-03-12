package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.WorkspaceRole;

public record AddWorkspaceMemberCommand(
    String workspaceId,
    String email,
    WorkspaceRole role,
    String requesterId) {
}
