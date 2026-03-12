package com.schemafy.domain.project.application.port.in;

public record RejectWorkspaceInvitationCommand(
    String invitationId,
    String requesterId) {
}
