package com.schemafy.core.project.application.port.in;

public record RejectWorkspaceInvitationCommand(
    String invitationId,
    String requesterId) {
}
