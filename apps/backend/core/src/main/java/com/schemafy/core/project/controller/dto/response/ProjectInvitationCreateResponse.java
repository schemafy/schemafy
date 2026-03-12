package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.domain.project.domain.Invitation;

public record ProjectInvitationCreateResponse(
    String id,
    String projectId,
    String workspaceId,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant createdAt) {

  public static ProjectInvitationCreateResponse of(Invitation invitation) {
    return new ProjectInvitationCreateResponse(
        invitation.getId(),
        invitation.getTargetId(),
        invitation.getParentId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt());
  }

}
