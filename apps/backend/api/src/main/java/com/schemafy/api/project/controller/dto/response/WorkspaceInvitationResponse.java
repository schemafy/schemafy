package com.schemafy.api.project.controller.dto.response;

import java.time.Instant;

import com.schemafy.core.project.application.port.in.InvitationSummary;

public record WorkspaceInvitationResponse(
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
    Instant resolvedAt,
    Instant createdAt) {

  public static WorkspaceInvitationResponse of(InvitationSummary invitation) {
    return new WorkspaceInvitationResponse(
        invitation.id(),
        invitation.targetType(),
        invitation.targetId(),
        invitation.targetName(),
        invitation.targetDescription(),
        invitation.invitedEmail(),
        invitation.invitedRole(),
        displayInvitedBy(invitation.invitedBy()),
        invitation.status(),
        invitation.expiresAt(),
        invitation.resolvedAt(),
        invitation.createdAt());
  }

  private static String displayInvitedBy(String invitedBy) {
    return invitedBy == null ? "Unknown" : invitedBy;
  }

}
