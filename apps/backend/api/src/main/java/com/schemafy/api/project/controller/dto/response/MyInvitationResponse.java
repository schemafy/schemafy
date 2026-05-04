package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.application.port.in.InvitationSummary;

public record MyInvitationResponse(
    String id,
    String type,
    String targetId,
    String targetName,
    String targetDescription,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant createdAt) {

  public static MyInvitationResponse of(InvitationSummary invitation) {
    return new MyInvitationResponse(
        invitation.id(),
        invitation.targetType(),
        invitation.targetId(),
        invitation.targetName(),
        invitation.targetDescription(),
        invitation.invitedEmail(),
        invitation.invitedRole(),
        invitation.invitedBy(),
        invitation.status(),
        invitation.expiresAt(),
        invitation.createdAt());
  }

}
