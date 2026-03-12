package com.schemafy.domain.project.application.port.in;

public record AcceptWorkspaceInvitationCommand(
    String invitationId,
    String requesterId) {
}
