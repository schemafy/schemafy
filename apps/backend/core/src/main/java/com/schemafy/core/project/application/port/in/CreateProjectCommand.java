package com.schemafy.core.project.application.port.in;

public record CreateProjectCommand(
    String workspaceId,
    String name,
    String description,
    String requesterId) {
}
