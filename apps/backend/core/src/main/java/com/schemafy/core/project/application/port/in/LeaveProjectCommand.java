package com.schemafy.core.project.application.port.in;

public record LeaveProjectCommand(
    String projectId,
    String requesterId) {
}
