package com.schemafy.core.project.application.port.in;

public record AccessShareLinkQuery(
    String code,
    String userId,
    String ipAddress,
    String userAgent) {
}
