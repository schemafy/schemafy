package com.schemafy.domain.project.application.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.project.application.port.in.RejectWorkspaceInvitationCommand;
import com.schemafy.domain.project.application.port.in.RejectWorkspaceInvitationUseCase;
import com.schemafy.domain.project.application.port.out.InvitationPort;
import com.schemafy.domain.project.domain.exception.ProjectErrorCode;
import com.schemafy.domain.user.application.port.out.FindUserByIdPort;
import com.schemafy.domain.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
class RejectWorkspaceInvitationService
    implements RejectWorkspaceInvitationUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      RejectWorkspaceInvitationService.class);

  private final TransactionalOperator transactionalOperator;
  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;
  private final FindUserByIdPort findUserByIdPort;

  @Override
  public Mono<Void> rejectWorkspaceInvitation(
      RejectWorkspaceInvitationCommand command) {
    return findUserByIdPort.findUserById(command.requesterId())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)))
        .flatMap(user -> workspaceInvitationHelper.findInvitationOrThrow(
            command.invitationId())
            .flatMap(invitation -> {
              if (!invitation.getTargetTypeAsEnum().isWorkspace()) {
                return Mono.error(new DomainException(
                    ProjectErrorCode.INVITATION_TYPE_MISMATCH));
              }

              invitation.validateInvitedEmailMatches(user.email());
              invitation.reject();
              return invitationPort.save(invitation);
            }))
        .as(transactionalOperator::transactional)
        .retryWhen(Retry.max(3)
            .filter(OptimisticLockingFailureException.class::isInstance)
            .doBeforeRetry(signal -> log.warn(
                "Retrying due to concurrent modification: invitationId={}",
                command.invitationId())))
        .onErrorMap(OptimisticLockingFailureException.class,
            error -> new DomainException(
                ProjectErrorCode.INVITATION_CONCURRENT_PROCESSED))
        .then();
  }

}
