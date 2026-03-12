package com.schemafy.domain.project.application.port.in;

public record LeaveProjectCommand(
    String projectId,
    String requesterId) {
}
