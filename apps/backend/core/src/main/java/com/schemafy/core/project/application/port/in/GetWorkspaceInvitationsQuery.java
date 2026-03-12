package com.schemafy.core.project.application.port.in;

public record GetWorkspaceInvitationsQuery(
    String workspaceId,
    String requesterId,
    int page,
    int size) {
}
