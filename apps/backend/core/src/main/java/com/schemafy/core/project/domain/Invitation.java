package com.schemafy.core.project.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.BaseEntity;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("invitations")
public class Invitation extends BaseEntity {

  private static final long EXPIRATION_DAYS = 7;

  private String targetType;
  private String targetId;
  private String parentId;
  private String invitedEmail;
  private String invitedRole;
  private String invitedBy;
  private String status;
  private Instant expiresAt;
  private Instant resolvedAt;

  @Version
  private Integer version;

  public static Invitation createWorkspaceInvitation(
      String id,
      String workspaceId,
      String invitedEmail,
      WorkspaceRole role,
      String invitedBy) {
    Invitation invitation = new Invitation(
        InvitationType.WORKSPACE.name(),
        workspaceId,
        null,
        invitedEmail.toLowerCase(),
        role.name(),
        invitedBy,
        InvitationStatus.PENDING.name(),
        Instant.now().plus(EXPIRATION_DAYS, ChronoUnit.DAYS),
        null,
        0);
    invitation.setId(id);
    return invitation;
  }

  public static Invitation createProjectInvitation(
      String id,
      String projectId,
      String workspaceId,
      String invitedEmail,
      ProjectRole role,
      String invitedBy) {
    Invitation invitation = new Invitation(
        InvitationType.PROJECT.name(),
        projectId,
        workspaceId,
        invitedEmail.toLowerCase(),
        role.name(),
        invitedBy,
        InvitationStatus.PENDING.name(),
        Instant.now().plus(EXPIRATION_DAYS, ChronoUnit.DAYS),
        null,
        0);
    invitation.setId(id);
    return invitation;
  }

  public boolean isExpired() { return Instant.now().isAfter(this.expiresAt); }

  public InvitationStatus getStatusAsEnum() { return InvitationStatus.fromString(this.status); }

  public InvitationType getTargetTypeAsEnum() { return InvitationType.fromString(this.targetType); }

  public String getWorkspaceId() {
    if (getTargetTypeAsEnum().isWorkspace()) {
      return this.targetId;
    }
    return this.parentId;
  }

  public String getProjectId() {
    if (!getTargetTypeAsEnum().isProject()) {
      throw new DomainException(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return this.targetId;
  }

  public WorkspaceRole getWorkspaceRole() {
    if (!getTargetTypeAsEnum().isWorkspace()) {
      throw new DomainException(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return WorkspaceRole.fromString(this.invitedRole);
  }

  public ProjectRole getProjectRole() {
    if (!getTargetTypeAsEnum().isProject()) {
      throw new DomainException(ProjectErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return ProjectRole.fromString(this.invitedRole);
  }

  public void accept() {
    if (!getStatusAsEnum().isPending()) {
      ProjectErrorCode errorCode = getTargetTypeAsEnum().isWorkspace()
          ? ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED
          : ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED;
      throw new DomainException(errorCode);
    }
    if (isExpired()) {
      throw new DomainException(ProjectErrorCode.INVITATION_EXPIRED);
    }
    this.status = InvitationStatus.ACCEPTED.name();
    this.resolvedAt = Instant.now();
  }

  public void reject() {
    if (!getStatusAsEnum().isPending()) {
      ProjectErrorCode errorCode = getTargetTypeAsEnum().isWorkspace()
          ? ProjectErrorCode.WORKSPACE_INVITATION_ALREADY_PROCESSED
          : ProjectErrorCode.PROJECT_INVITATION_ALREADY_PROCESSED;
      throw new DomainException(errorCode);
    }
    this.status = InvitationStatus.REJECTED.name();
    this.resolvedAt = Instant.now();
  }

  public void validateInvitedEmailMatches(String email) {
    if (!this.invitedEmail.equals(email.toLowerCase())) {
      throw new DomainException(ProjectErrorCode.INVITATION_EMAIL_MISMATCH);
    }
  }

}
