package com.schemafy.core.project.application.port.in;

import com.schemafy.core.project.domain.WorkspaceRole;

public record UpdateWorkspaceMemberRoleCommand(
    String workspaceId,
    String targetUserId,
    WorkspaceRole role,
    String requesterId) {
}
