package com.schemafy.domain.project.application.port.in;

public record UpdateWorkspaceCommand(
    String workspaceId,
    String name,
    String description,
    String requesterId) {
}
