package com.schemafy.domain.project.application.port.in;

public record UpdateProjectCommand(
    String projectId,
    String name,
    String description,
    String requesterId) {
}
