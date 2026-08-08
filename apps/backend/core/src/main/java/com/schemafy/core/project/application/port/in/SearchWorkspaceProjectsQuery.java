package com.schemafy.core.project.application.port.in;

public record SearchWorkspaceProjectsQuery(
    String workspaceId,
    String requesterId,
    String search,
    int page,
    int size) {
}
