package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.InvitationType;
import com.schemafy.core.project.repository.vo.ProjectRole;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("invitations")
public class Invitation extends BaseEntity {

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

  private static final long EXPIRATION_DAYS = 7 * 24 * 60 * 60; // 7 days

  public static Invitation createWorkspaceInvitation(
      String workspaceId,
      String invitedEmail,
      WorkspaceRole role,
      String invitedBy) {
    Invitation invitation = new Invitation(
        InvitationType.WORKSPACE.getValue(),
        workspaceId,
        null,
        invitedEmail.toLowerCase(),
        role.getValue(),
        invitedBy,
        InvitationStatus.PENDING.getValue(),
        Instant.now().plusSeconds(EXPIRATION_DAYS),
        null,
        0);
    invitation.setId(UlidGenerator.generate());
    return invitation;
  }

  public static Invitation createProjectInvitation(
      String projectId,
      String workspaceId,
      String invitedEmail,
      ProjectRole role,
      String invitedBy) {
    Invitation invitation = new Invitation(
        InvitationType.PROJECT.getValue(),
        projectId,
        workspaceId,
        invitedEmail.toLowerCase(),
        role.getValue(),
        invitedBy,
        InvitationStatus.PENDING.getValue(),
        Instant.now().plusSeconds(EXPIRATION_DAYS),
        null,
        0);
    invitation.setId(UlidGenerator.generate());
    return invitation;
  }

  public boolean isExpired() { return Instant.now().isAfter(this.expiresAt); }

  public InvitationStatus getStatusAsEnum() { return InvitationStatus.fromValue(this.status); }

  public InvitationType getTargetTypeAsEnum() { return InvitationType.fromValue(this.targetType); }

  public String getWorkspaceId() {
    if (getTargetTypeAsEnum().isWorkspace()) {
      return this.targetId;
    }
    return this.parentId;
  }

  public String getProjectId() {
    if (!getTargetTypeAsEnum().isProject()) {
      throw new BusinessException(ErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return this.targetId;
  }

  public WorkspaceRole getWorkspaceRole() {
    if (!getTargetTypeAsEnum().isWorkspace()) {
      throw new BusinessException(ErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return WorkspaceRole.fromValue(this.invitedRole);
  }

  /** Get project role (only for PROJECT invitations)
   *
   * @throws BusinessException if not a project invitation */
  public ProjectRole getProjectRole() {
    if (!getTargetTypeAsEnum().isProject()) {
      throw new BusinessException(ErrorCode.INVITATION_TYPE_MISMATCH);
    }
    return ProjectRole.fromString(this.invitedRole);
  }

  public void accept() {
    if (!getStatusAsEnum().isPending()) {
      ErrorCode errorCode = getTargetTypeAsEnum().isWorkspace()
          ? ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION
          : ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION;
      throw new BusinessException(errorCode);
    }
    if (isExpired()) {
      throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
    }
    this.status = InvitationStatus.ACCEPTED.getValue();
    this.resolvedAt = Instant.now();
  }

  public void reject() {
    if (!getStatusAsEnum().isPending()) {
      ErrorCode errorCode = getTargetTypeAsEnum().isWorkspace()
          ? ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION
          : ErrorCode.PROJECT_INVITATION_ALREADY_MODIFICATION;
      throw new BusinessException(errorCode);
    }
    this.status = InvitationStatus.REJECTED.getValue();
    this.resolvedAt = Instant.now();
  }

  public void validateInvitedEmailMatches(String email) {
    if (!this.invitedEmail.equals(email.toLowerCase())) {
      throw new BusinessException(ErrorCode.INVITATION_EMAIL_MISMATCH);
    }
  }

  @Override
  public boolean isDeleted() { return deletedAt != null; }

}
