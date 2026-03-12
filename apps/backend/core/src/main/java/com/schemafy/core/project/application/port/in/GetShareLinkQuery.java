package com.schemafy.core.project.application.port.in;

public record GetShareLinkQuery(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
