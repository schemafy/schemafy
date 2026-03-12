package com.schemafy.core.project.application.port.in;

public record AcceptWorkspaceInvitationCommand(
    String invitationId,
    String requesterId) {
}
