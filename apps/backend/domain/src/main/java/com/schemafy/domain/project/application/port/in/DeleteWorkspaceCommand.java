package com.schemafy.domain.project.application.port.in;

public record DeleteWorkspaceCommand(
    String workspaceId,
    String requesterId) {
}
