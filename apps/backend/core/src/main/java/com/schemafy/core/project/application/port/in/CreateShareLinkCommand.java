package com.schemafy.core.project.application.port.in;

public record CreateShareLinkCommand(
    String projectId,
    String requesterId) {
}
