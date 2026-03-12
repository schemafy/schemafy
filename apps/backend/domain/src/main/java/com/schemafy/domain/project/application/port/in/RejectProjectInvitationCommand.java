package com.schemafy.domain.project.application.port.in;

public record RejectProjectInvitationCommand(
    String invitationId,
    String requesterId) {
}
