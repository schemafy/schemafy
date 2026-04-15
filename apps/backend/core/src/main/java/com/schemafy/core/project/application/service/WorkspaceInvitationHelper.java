package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class WorkspaceInvitationHelper {

  private static final Logger log = LoggerFactory.getLogger(
      WorkspaceInvitationHelper.class);

  private final InvitationPort invitationPort;
  private final WorkspacePort workspacePort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final FindUserByEmailPort findUserByEmailPort;
  private final UlidGeneratorPort ulidGeneratorPort;

  Mono<Invitation> findInvitationOrThrow(String invitationId) {
    return invitationPort.findByIdAndNotDeleted(invitationId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.INVITATION_NOT_FOUND)));
  }

  Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspacePort.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.NOT_FOUND)));
  }

  Mono<Void> validateAdmin(String workspaceId, String userId) {
    return workspaceMemberPort
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.ACCESS_DENIED)))
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(
                new DomainException(WorkspaceErrorCode.ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkDuplicatePendingInvitation(String workspaceId, Email email) {
    return invitationPort.countByTargetAndEmailAndStatus(
        InvitationType.WORKSPACE.name(),
        workspaceId,
        email.address(),
        InvitationStatus.PENDING.name())
        .flatMap(count -> {
          if (count > 0) {
            log.warn("Duplicate pending invitation: workspace={}, email={}",
                workspaceId, email.address());
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_ALREADY_EXISTS));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkNotAlreadyMember(String workspaceId, String userId) {
    return workspaceMemberPort
        .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
          }
          return Mono.empty();
        });
  }

  Mono<Void> checkNotAlreadyMemberByEmail(String workspaceId, Email email) {
    return findUserByEmailPort.findUserByEmail(email.address())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> workspaceMemberPort
            .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, user.id())
            .flatMap(exists -> {
              if (exists) {
                return Mono.error(new DomainException(
                    ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
              }
              return Mono.empty();
            }))
        .then();
  }

  Mono<WorkspaceMember> saveOrRestoreWorkspaceMember(
      String workspaceId,
      String userId,
      WorkspaceRole role) {
    return workspaceMemberPort.findByWorkspaceIdAndUserId(workspaceId,
        userId)
        .flatMap(existing -> {
          if (!existing.isDeleted()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
          }
          existing.restore();
          existing.updateRole(role);
          return workspaceMemberPort.save(existing);
        })
        .switchIfEmpty(Mono.defer(() -> Mono
            .fromCallable(ulidGeneratorPort::generate)
            .map(id -> WorkspaceMember.create(id, workspaceId, userId, role))
            .flatMap(workspaceMemberPort::save)));
  }

}
