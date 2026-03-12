package com.schemafy.domain.project.application.port.in;

import com.schemafy.domain.project.domain.WorkspaceRole;

public record UpdateWorkspaceMemberRoleCommand(
    String workspaceId,
    String targetUserId,
    WorkspaceRole role,
    String requesterId) {
}
