package com.schemafy.core.project.application.port.in;

public record RevokeShareLinkCommand(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
