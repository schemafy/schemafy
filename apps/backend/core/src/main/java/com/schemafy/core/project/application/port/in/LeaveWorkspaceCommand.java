package com.schemafy.core.project.application.port.in;

public record LeaveWorkspaceCommand(
    String workspaceId,
    String requesterId) {
}
