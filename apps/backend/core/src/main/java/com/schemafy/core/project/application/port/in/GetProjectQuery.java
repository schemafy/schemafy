package com.schemafy.core.project.application.port.in;

public record GetProjectQuery(
    String projectId,
    String requesterId) {
}
