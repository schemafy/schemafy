package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.AcceptProjectInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationStatus;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
class AcceptProjectInvitationService implements AcceptProjectInvitationUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      AcceptProjectInvitationService.class);

  private static final String CLEANUP_REASON_ALREADY_MEMBER = "already_member_accept";
  private static final String CLEANUP_REASON_ACCEPTED_SIBLING = "accepted_sibling_pending";
  private static final String CLEANUP_SOURCE_ACCEPT_PROJECT_INVITATION = "accept_project_invitation";

  private final TransactionalOperator transactionalOperator;
  private final InvitationPort invitationPort;
  private final ProjectInvitationHelper projectInvitationHelper;
  private final FindUserByIdPort findUserByIdPort;
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<ProjectMember> acceptProjectInvitation(
      AcceptProjectInvitationCommand command) {
    return findUserByIdPort.findUserById(command.requesterId())
        .switchIfEmpty(Mono.error(
            new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> projectInvitationHelper.findInvitationOrThrow(
            command.invitationId())
            .flatMap(invitation -> projectAccessHelper
                .findProjectById(invitation.getProjectId())
                .flatMap(project -> Mono.defer(() -> acceptProjectInvitationWithCleanup(
                    command, project.getWorkspaceId(),
                    invitation.getProjectId(), user.email())
                    .as(transactionalOperator::transactional))
                    .flatMap(AcceptProjectInvitationResult::toMono))))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}",
                command.invitationId())))
        .onErrorMap(OptimisticLockingFailureException.class,
            error -> new DomainException(
                ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED));
  }

  private Mono<AcceptProjectInvitationResult> acceptProjectInvitationWithCleanup(
      AcceptProjectInvitationCommand command,
      String workspaceId,
      String projectId,
      String requesterEmail) {
    return projectAccessHelper.requireProjectWithinWorkspace(
        workspaceId, projectId)
        .then(projectInvitationHelper.findInvitationOrThrow(
            command.invitationId()))
        .flatMap(invitation -> {
          if (!invitation.getTargetTypeAsEnum().isProject()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_TYPE_MISMATCH));
          }

          invitation.validateInvitedEmailMatches(requesterEmail);
          return projectInvitationHelper.checkNotAlreadyProjectMember(
              projectId,
              command.requesterId())
              .then(Mono.defer(() -> projectInvitationHelper
                  .saveOrRestoreProjectMember(
                      projectId,
                      command.requesterId(),
                      invitation.getProjectRole())
                  .onErrorResume(DataIntegrityViolationException.class,
                      error -> {
                        log.warn(
                            "Concurrent member creation on invitation accept: invitationId={}",
                            command.invitationId());
                        return Mono.error(new DomainException(
                            ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
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
                                CLEANUP_SOURCE_ACCEPT_PROJECT_INVITATION,
                                invitation.getTargetType(),
                                invitation.getTargetId(),
                                command.requesterId())))
                        .thenReturn(AcceptProjectInvitationResult
                            .success(savedMember));
                  })))
              .onErrorResume(error -> {
                if (!(error instanceof DomainException domainException
                    && domainException.getErrorCode() == ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT)) {
                  return Mono.error(error);
                }
                return cancelAlreadyMemberInvitation(
                    invitation, command.requesterId())
                    .thenReturn(AcceptProjectInvitationResult
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
            CLEANUP_SOURCE_ACCEPT_PROJECT_INVITATION,
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
                CLEANUP_SOURCE_ACCEPT_PROJECT_INVITATION,
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
                CLEANUP_SOURCE_ACCEPT_PROJECT_INVITATION,
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

  private record AcceptProjectInvitationResult(
      ProjectMember member,
      ProjectErrorCode errorCode) {

    static AcceptProjectInvitationResult success(ProjectMember member) {
      return new AcceptProjectInvitationResult(member, null);
    }

    static AcceptProjectInvitationResult duplicateMembership() {
      return new AcceptProjectInvitationResult(
          null,
          ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT);
    }

    Mono<ProjectMember> toMono() {
      if (errorCode == null) {
        return Mono.just(member);
      }
      return Mono.error(new DomainException(errorCode));
    }

  }

}
