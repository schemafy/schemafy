package com.schemafy.domain.project.application.port.in;

public record DeleteShareLinkCommand(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
