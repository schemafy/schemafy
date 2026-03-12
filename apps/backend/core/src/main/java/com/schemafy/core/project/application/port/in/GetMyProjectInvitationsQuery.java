package com.schemafy.core.project.application.port.in;

public record GetMyProjectInvitationsQuery(
    String requesterId,
    int page,
    int size) {
}
