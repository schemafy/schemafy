package com.schemafy.domain.project.application.port.in;

public record RemoveWorkspaceMemberCommand(
    String workspaceId,
    String targetUserId,
    String requesterId) {
}
