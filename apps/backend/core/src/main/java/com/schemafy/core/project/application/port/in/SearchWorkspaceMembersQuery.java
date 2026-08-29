package com.schemafy.core.project.application.port.in;

public record SearchWorkspaceMembersQuery(
    String workspaceId,
    String requesterId,
    String search,
    int page,
    int size) {
}
