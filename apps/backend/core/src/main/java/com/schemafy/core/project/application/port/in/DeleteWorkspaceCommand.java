package com.schemafy.core.project.application.port.in;

public record DeleteWorkspaceCommand(
    String workspaceId,
    String requesterId) {
}
