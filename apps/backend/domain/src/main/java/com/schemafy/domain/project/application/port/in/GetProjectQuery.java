package com.schemafy.domain.project.application.port.in;

public record GetProjectQuery(
    String projectId,
    String requesterId) {
}
