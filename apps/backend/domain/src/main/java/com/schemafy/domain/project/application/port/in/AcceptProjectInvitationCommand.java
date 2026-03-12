package com.schemafy.domain.project.application.port.in;

public record AcceptProjectInvitationCommand(
    String invitationId,
    String requesterId) {
}
