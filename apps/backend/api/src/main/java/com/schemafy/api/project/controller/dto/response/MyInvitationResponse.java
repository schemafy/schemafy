package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.domain.Invitation;

public record MyInvitationResponse(
    String id,
    String type,
    String targetId,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant createdAt) {

  public static MyInvitationResponse of(Invitation invitation) {
    return new MyInvitationResponse(
        invitation.getId(),
        invitation.getTargetType(),
        invitation.getTargetId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt());
  }

}
