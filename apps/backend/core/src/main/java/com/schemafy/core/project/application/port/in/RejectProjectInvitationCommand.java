package com.schemafy.core.project.application.port.in;

public record RejectProjectInvitationCommand(
    String invitationId,
    String requesterId) {
}
