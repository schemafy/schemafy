package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.WorkspaceRole;

public record AddWorkspaceMemberCommand(
    String workspaceId,
    String email,
    WorkspaceRole role,
    String requesterId) {
}
