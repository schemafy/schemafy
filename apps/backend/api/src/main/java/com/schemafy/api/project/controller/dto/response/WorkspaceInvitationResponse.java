package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.domain.Invitation;

public record WorkspaceInvitationResponse(
    String id,
    String workspaceId,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant createdAt) {

  public static WorkspaceInvitationResponse of(Invitation invitation) {
    return new WorkspaceInvitationResponse(
        invitation.getId(),
        invitation.getWorkspaceId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getResolvedAt(),
        invitation.getCreatedAt());
  }

}
