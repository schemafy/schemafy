package com.schemafy.domain.project.application.port.in;

public record RevokeShareLinkCommand(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
