package com.schemafy.core.project.application.port.in;

public record UpdateProjectShareLinkCommand(
    String projectId,
    boolean isActive,
    String requesterId) {
}
