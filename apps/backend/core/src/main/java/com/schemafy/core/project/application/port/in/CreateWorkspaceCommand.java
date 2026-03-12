package com.schemafy.core.project.application.port.in;

public record CreateWorkspaceCommand(
    String name,
    String description,
    String requesterId) {
}
