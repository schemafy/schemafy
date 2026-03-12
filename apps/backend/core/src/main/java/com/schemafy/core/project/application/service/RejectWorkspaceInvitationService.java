package com.schemafy.core.project.application.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.RejectWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByIdPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

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
