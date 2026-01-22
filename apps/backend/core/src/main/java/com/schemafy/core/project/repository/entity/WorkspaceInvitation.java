package com.schemafy.core.project.repository.entity;

import java.time.Instant;

import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.project.repository.vo.InvitationStatus;
import com.schemafy.core.project.repository.vo.WorkspaceRole;
import com.schemafy.core.ulid.generator.UlidGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("workspace_invitations")
public class WorkspaceInvitation extends BaseEntity {

  private String workspaceId;
  private String invitedEmail;
  private String invitedRole;
  private String invitedBy;
  private String status;
  private Instant expiresAt;
  private Instant resolvedAt;

  private static final long EXPIRATION_DAYS = 7 * 24 * 60 * 60; // 7 days

  @Version
  private Integer version;

  public static WorkspaceInvitation create(
      String workspaceId,
      String invitedEmail,
      WorkspaceRole role,
      String invitedBy) {
    WorkspaceInvitation invitation = new WorkspaceInvitation(
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

  public WorkspaceRole getRoleAsEnum() { return WorkspaceRole.fromValue(this.invitedRole); }

  public void accept() {
    if (isExpired()) {
      throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
    }
    if (!getStatusAsEnum().isPending()) {
      throw new BusinessException(ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION);
    }
    this.status = InvitationStatus.ACCEPTED.getValue();
    this.resolvedAt = Instant.now();
  }

  public void reject() {
    if (!getStatusAsEnum().isPending()) {
      throw new BusinessException(ErrorCode.WORKSPACE_INVITATION_ALREADY_MODIFICATION);
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
