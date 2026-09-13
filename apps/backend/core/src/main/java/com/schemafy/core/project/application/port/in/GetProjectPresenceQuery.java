package com.schemafy.core.project.application.port.in;

public record GetProjectPresenceQuery(
    String projectId,
    String requesterId) {
}
