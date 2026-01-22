package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.ProjectInvitation;

public record ProjectInvitationResponse(
    String id,
    String workspaceId,
    String projectId,
    String invitedEmail,
    String invitedRole,
    String invitedBy,
    String status,
    Instant expiresAt,
    Instant resolvedAt,
    Instant createdAt) {

  public static ProjectInvitationResponse of(ProjectInvitation invitation) {
    return new ProjectInvitationResponse(
        invitation.getId(),
        invitation.getWorkspaceId(),
        invitation.getProjectId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getResolvedAt(),
        invitation.getCreatedAt());
  }

}
