package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.domain.Invitation;

public record WorkspaceInvitationCreateResponse(
    String id,
    String workspaceId,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant createdAt) {

  public static WorkspaceInvitationCreateResponse of(Invitation invitation) {
    return new WorkspaceInvitationCreateResponse(
        invitation.getId(),
        invitation.getWorkspaceId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt());
  }

}
