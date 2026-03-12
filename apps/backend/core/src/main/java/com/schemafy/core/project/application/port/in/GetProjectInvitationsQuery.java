package com.schemafy.core.project.application.port.in;

public record GetProjectInvitationsQuery(
    String projectId,
    String requesterId,
    int page,
    int size) {
}
