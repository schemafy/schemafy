package com.schemafy.domain.project.application.port.in;

public record LeaveWorkspaceCommand(
    String workspaceId,
    String requesterId) {
}
