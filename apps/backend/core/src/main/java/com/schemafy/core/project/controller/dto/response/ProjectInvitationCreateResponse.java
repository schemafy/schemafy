package com.schemafy.core.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.repository.entity.ProjectInvitation;

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

  public static ProjectInvitationCreateResponse of(ProjectInvitation invitation) {
    return new ProjectInvitationCreateResponse(
        invitation.getId(),
        invitation.getProjectId(),
        invitation.getWorkspaceId(),
        invitation.getInvitedEmail(),
        invitation.getInvitedRole(),
        invitation.getInvitedBy(),
        invitation.getStatus(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt());
  }

}
