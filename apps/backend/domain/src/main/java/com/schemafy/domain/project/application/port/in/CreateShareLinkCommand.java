package com.schemafy.domain.project.application.port.in;

public record CreateShareLinkCommand(
    String projectId,
    String requesterId) {
}
