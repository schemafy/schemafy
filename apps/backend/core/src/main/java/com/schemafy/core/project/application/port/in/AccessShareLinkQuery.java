package com.schemafy.core.project.application.port.in;

public record AccessShareLinkQuery(
    String shareLinkId,
    String userId,
    String ipAddress,
    String userAgent) {
}
