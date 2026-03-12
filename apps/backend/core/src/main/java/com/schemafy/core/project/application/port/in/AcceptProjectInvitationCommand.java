package com.schemafy.core.project.application.port.in;

public record AcceptProjectInvitationCommand(
    String invitationId,
    String requesterId) {
}
