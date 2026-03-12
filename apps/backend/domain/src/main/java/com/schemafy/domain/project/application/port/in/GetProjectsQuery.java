package com.schemafy.domain.project.application.port.in;

public record GetProjectsQuery(
    String workspaceId,
    String requesterId,
    int page,
    int size) {
}
