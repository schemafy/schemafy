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
                .flatMap(project -> Mono.defer(() -> acceptProjectInvitationWithinWriteScope(
                    command, project.getWorkspaceId(),
                    invitation.getProjectId(), user.email())
                    .as(transactionalOperator::transactional)))))
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}",
                command.invitationId())))
        .onErrorMap(OptimisticLockingFailureException.class,
            error -> new DomainException(
                ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED));
  }

  private Mono<ProjectMember> acceptProjectInvitationWithinWriteScope(
      AcceptProjectInvitationCommand command,
      String workspaceId,
      String projectId,
      String requesterEmail) {
    return projectAccessHelper.requireProjectWithinWorkspaceForWrite(
        workspaceId, projectId)
        .then(projectInvitationHelper.findInvitationOrThrow(command.invitationId()))
        .flatMap(invitation -> {
          if (!invitation.getTargetTypeAsEnum().isProject()) {
            return Mono.error(new DomainException(
                ProjectErrorCode.INVITATION_TYPE_MISMATCH));
          }

          invitation.validateInvitedEmailMatches(requesterEmail);
          return projectInvitationHelper.checkNotAlreadyProjectMember(
              projectId,
              command.requesterId())
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
                    .then(projectInvitationHelper.saveOrRestoreProjectMember(
                        projectId,
                        command.requesterId(),
                        invitation.getProjectRole()))
                    .onErrorResume(DataIntegrityViolationException.class,
                        error -> {
                          log.warn(
                              "Concurrent member creation on invitation accept: invitationId={}",
                              command.invitationId());
                          return Mono.error(new DomainException(
                              ProjectErrorCode.INVITATION_DUPLICATE_MEMBERSHIP_PROJECT));
                        });
              }));
        });
  }

}
