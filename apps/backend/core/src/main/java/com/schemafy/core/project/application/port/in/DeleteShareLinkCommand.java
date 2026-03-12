package com.schemafy.core.project.application.port.in;

public record DeleteShareLinkCommand(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
