package com.schemafy.core.project.application.port.in;

public record GetMyWorkspaceInvitationsQuery(
    String requesterId,
    int page,
    int size) {
}
