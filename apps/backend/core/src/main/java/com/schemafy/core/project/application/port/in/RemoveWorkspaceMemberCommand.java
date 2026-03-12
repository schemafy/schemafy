package com.schemafy.core.project.application.port.in;

public record RemoveWorkspaceMemberCommand(
    String workspaceId,
    String targetUserId,
    String requesterId) {
}
