package com.schemafy.domain.project.application.port.in;

public record CreateWorkspaceCommand(
    String name,
    String description,
    String requesterId) {
}
