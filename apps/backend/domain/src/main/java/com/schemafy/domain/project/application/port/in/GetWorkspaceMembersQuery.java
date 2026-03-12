package com.schemafy.domain.project.application.port.in;

public record GetWorkspaceMembersQuery(
    String workspaceId,
    String requesterId,
    int page,
    int size) {
}
