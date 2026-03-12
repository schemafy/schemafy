package com.schemafy.domain.project.application.port.in;

public record GetShareLinkQuery(
    String projectId,
    String shareLinkId,
    String requesterId) {
}
