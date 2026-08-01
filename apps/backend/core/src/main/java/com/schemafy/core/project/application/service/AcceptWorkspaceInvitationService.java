package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
class AcceptWorkspaceInvitationService
    implements AcceptWorkspaceInvitationUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      AcceptWorkspaceInvitationService.class);

  private final WorkspaceMutationGuard workspaceMutationGuard;
  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<WorkspaceMember> acceptWorkspaceInvitation(
      AcceptWorkspaceInvitationCommand command) {
    return findUserByIdPort.findUserById(command.requesterId())
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> workspaceInvitationHelper.findInvitationOrThrow(
            command.invitationId())
            .flatMap(preLockInvitation -> {
              if (!preLockInvitation.getTargetTypeAsEnum().isWorkspace()) {
                return Mono.error(new DomainException(
                    ProjectErrorCode.INVITATION_TYPE_MISMATCH));
              }
              return workspaceMutationGuard.protectExclusive(
                  preLockInvitation.getWorkspaceId(),
                  () -> acceptLockedInvitation(command, user));
            }))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}",
                command.invitationId()))
            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
        .onErrorMap(OptimisticLockingFailureException.class,
            error -> new DomainException(
                ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED));
  }

  private Mono<WorkspaceMember> acceptLockedInvitation(
      AcceptWorkspaceInvitationCommand command,
      User user) {
    return workspaceInvitationHelper.findInvitationOrThrow(command.invitationId())
        .flatMap(invitation -> {
          if (!invitation.getTargetTypeAsEnum().isWorkspace()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_TYPE_MISMATCH));
          }

          invitation.validateInvitedEmailMatches(user.email());
          return workspaceInvitationHelper.findWorkspaceOrThrow(
              invitation.getWorkspaceId())
              .then(workspaceInvitationHelper.checkNotAlreadyMember(
                  invitation.getWorkspaceId(), command.requesterId()))
              .then(Mono.defer(() -> {
                invitation.accept();
                return invitationPort.save(invitation)
                    .then(invitationPort.updateStatusByTargetAndEmail(
                        invitation.getTargetType(),
                        invitation.getTargetId(),
                        invitation.getInvitedEmail(),
                        InvitationStatus.CANCELLED.name(),
                        InvitationStatus.PENDING.name(),
                        invitation.getId()))
                    .then(workspaceInvitationHelper.saveOrRestoreWorkspaceMember(
                        invitation.getWorkspaceId(),
                        command.requesterId(),
                        invitation.getWorkspaceRole()))
                    .onErrorResume(DataIntegrityViolationException.class, error -> {
                      log.warn(
                          "Concurrent member creation on invitation accept: invitationId={}",
                          command.invitationId());
                      return Mono.error(new DomainException(
                          ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
                    })
                    .flatMap(savedMember -> projectMembershipPropagationHelper
                        .syncProjectMembershipsForWorkspaceRole(
                            invitation.getWorkspaceId(),
                            command.requesterId(),
                            invitation.getWorkspaceRole())
                        .thenReturn(savedMember));
              }));
        });
  }

}
