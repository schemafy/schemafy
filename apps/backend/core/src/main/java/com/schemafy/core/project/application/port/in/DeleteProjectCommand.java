package com.schemafy.core.project.application.port.in;

public record DeleteProjectCommand(
    String projectId,
    String requesterId) {
}
