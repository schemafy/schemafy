package com.schemafy.domain.project.application.port.in;

public record GetProjectInvitationsQuery(
    String projectId,
    String requesterId,
    int page,
    int size) {
}
