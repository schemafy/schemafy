package com.schemafy.domain.project.application.port.in;

public record AccessShareLinkQuery(
    String code,
    String userId,
    String ipAddress,
    String userAgent) {
}
