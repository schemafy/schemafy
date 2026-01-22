package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.WorkspaceInvitation;

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

  public static WorkspaceInvitationResponse of(WorkspaceInvitation invitation) {
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
