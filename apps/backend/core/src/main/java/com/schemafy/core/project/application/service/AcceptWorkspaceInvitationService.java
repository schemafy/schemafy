package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
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
  private static final String CLEANUP_REASON_ALREADY_MEMBER = "already_member_accept";
  private static final String CLEANUP_REASON_ACCEPTED_SIBLING = "accepted_sibling_pending";
  private static final String CLEANUP_REASON_WORKSPACE_MEMBER_MATERIALIZED = "workspace_member_materialized";
  private static final String CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION = "accept_workspace_invitation";

  private final TransactionalOperator transactionalOperator;
  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;
  private final FindUserByIdPort findUserByIdPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  public Mono<WorkspaceMember> acceptWorkspaceInvitation(
      AcceptWorkspaceInvitationCommand command) {
    return findUserByIdPort.findUserById(command.requesterId())
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> workspaceInvitationHelper.findInvitationOrThrow(
            command.invitationId())
            .flatMap(invitation -> Mono.defer(() -> acceptWorkspaceInvitationWithCleanup(
                command, invitation.getWorkspaceId(), user.email())
                .as(transactionalOperator::transactional))
                .flatMap(AcceptWorkspaceInvitationResult::toMono)))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}",
                command.invitationId())))
        .onErrorMap(OptimisticLockingFailureException.class,
            error -> new DomainException(
                ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED));
  }

  private Mono<AcceptWorkspaceInvitationResult> acceptWorkspaceInvitationWithCleanup(
      AcceptWorkspaceInvitationCommand command,
      String workspaceId,
      String requesterEmail) {
    return workspaceAccessHelper.findWorkspaceOrThrow(workspaceId)
        .then(workspaceInvitationHelper.findInvitationOrThrow(
            command.invitationId()))
        .flatMap(invitation -> {
          if (!invitation.getTargetTypeAsEnum().isWorkspace()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_TYPE_MISMATCH));
          }

          invitation.validateInvitedEmailMatches(requesterEmail);
          return workspaceInvitationHelper.checkNotAlreadyMember(
              invitation.getWorkspaceId(), command.requesterId())
              .then(Mono.defer(() -> workspaceInvitationHelper
                  .saveOrRestoreWorkspaceMember(
                      invitation.getWorkspaceId(),
                      command.requesterId(),
                      invitation.getWorkspaceRole())
                  .onErrorResume(DataIntegrityViolationException.class,
                      error -> {
                        log.warn(
                            "Concurrent member creation on invitation accept: invitationId={}",
                            command.invitationId());
                        return Mono.error(new DomainException(
                            ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER));
                      })
                  .flatMap(savedMember -> {
                    invitation.accept();
                    return invitationPort.save(invitation)
                        .then(invitationPort.updateStatusByTargetAndEmail(
                            invitation.getTargetType(),
                            invitation.getTargetId(),
                            invitation.getInvitedEmail(),
                            InvitationStatus.CANCELLED.name(),
                            InvitationStatus.PENDING.name(),
                            invitation.getId())
                            .doOnNext(count -> logCleanupIfAny(
                                count,
                                CLEANUP_REASON_ACCEPTED_SIBLING,
                                CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION,
                                invitation.getTargetType(),
                                invitation.getTargetId(),
                                command.requesterId())))
                        .then(projectMembershipPropagationHelper
                            .syncProjectMembershipsForWorkspaceRole(
                                invitation.getWorkspaceId(),
                                command.requesterId(),
                                invitation.getWorkspaceRole())
                            .then(invitationPort
                                .cancelPendingProjectInvitationsByWorkspaceIdAndEmail(
                                    invitation.getWorkspaceId(),
                                    invitation.getInvitedEmail())
                                .doOnNext(count -> logCleanupIfAny(
                                    count,
                                    CLEANUP_REASON_WORKSPACE_MEMBER_MATERIALIZED,
                                    CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION,
                                    "PROJECT",
                                    invitation.getWorkspaceId(),
                                    command.requesterId())))
                            .thenReturn(AcceptWorkspaceInvitationResult
                                .success(savedMember)));
                  })))
              .onErrorResume(error -> {
                if (!(error instanceof DomainException domainException
                    && domainException.getErrorCode() == ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER)) {
                  return Mono.error(error);
                }
                return cancelAlreadyMemberInvitation(
                    invitation, command.requesterId())
                    .thenReturn(AcceptWorkspaceInvitationResult
                        .duplicateMembership());
              });
        });
  }

  private Mono<Void> cancelAlreadyMemberInvitation(
      Invitation invitation,
      String userId) {
    invitation.cancel();
    return invitationPort.save(invitation)
        .doOnSuccess(saved -> log.info(
            "Invitation cleanup: reason={}, source={}, targetType={}, targetId={}, userId={}, invitationId={}",
            CLEANUP_REASON_ALREADY_MEMBER,
            CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION,
            saved.getTargetType(),
            saved.getTargetId(),
            userId,
            saved.getId()))
        .then(invitationPort.updateStatusByTargetAndEmail(
            invitation.getTargetType(),
            invitation.getTargetId(),
            invitation.getInvitedEmail(),
            InvitationStatus.CANCELLED.name(),
            InvitationStatus.PENDING.name(),
            invitation.getId())
            .doOnNext(count -> logCleanupIfAny(
                count,
                CLEANUP_REASON_ALREADY_MEMBER,
                CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION,
                invitation.getTargetType(),
                invitation.getTargetId(),
                userId)))
        .then(invitationPort.cancelExpiredPendingInvitationsByTargetAndEmail(
            invitation.getTargetType(),
            invitation.getTargetId(),
            invitation.getInvitedEmail())
            .doOnNext(count -> logCleanupIfAny(
                count,
                CLEANUP_REASON_ALREADY_MEMBER,
                CLEANUP_SOURCE_ACCEPT_WORKSPACE_INVITATION,
                invitation.getTargetType(),
                invitation.getTargetId(),
                userId)))
        .then();
  }

  private void logCleanupIfAny(
      long cancelledCount,
      String reason,
      String source,
      String targetType,
      String targetId,
      String userId) {
    if (cancelledCount > 0) {
      log.info(
          "Invitation cleanup: reason={}, source={}, targetType={}, targetId={}, userId={}, cancelledCount={}",
          reason,
          source,
          targetType,
          targetId,
          userId,
          cancelledCount);
    }
  }

  private record AcceptWorkspaceInvitationResult(
      WorkspaceMember member,
      ProjectErrorCode errorCode) {

    static AcceptWorkspaceInvitationResult success(WorkspaceMember member) {
      return new AcceptWorkspaceInvitationResult(member, null);
    }

    static AcceptWorkspaceInvitationResult duplicateMembership() {
      return new AcceptWorkspaceInvitationResult(
          null,
          ProjectErrorCode.INVITATION_DUPLICATE_WORKSPACE_MEMBER);
    }

    Mono<WorkspaceMember> toMono() {
      if (errorCode == null) {
        return Mono.just(member);
      }
      return Mono.error(new DomainException(errorCode));
    }

  }

}
