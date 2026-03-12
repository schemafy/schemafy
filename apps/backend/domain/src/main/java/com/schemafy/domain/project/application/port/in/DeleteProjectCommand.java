package com.schemafy.domain.project.application.port.in;

public record DeleteProjectCommand(
    String projectId,
    String requesterId) {
}
