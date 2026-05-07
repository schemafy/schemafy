package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationType;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.Email;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateWorkspaceInvitationService
    implements CreateWorkspaceInvitationUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      CreateWorkspaceInvitationService.class);
  private static final String CLEANUP_REASON_EXPIRED_PENDING = "expired_pending";
  private static final String CLEANUP_SOURCE_CREATE_WORKSPACE_INVITATION = "create_workspace_invitation";

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;

  @Override
  public Mono<Invitation> createWorkspaceInvitation(
      CreateWorkspaceInvitationCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> workspaceInvitationHelper.validateAdmin(
            command.workspaceId(), command.requesterId())
            .then(workspaceInvitationHelper.findWorkspaceOrThrow(
                command.workspaceId()))
            .then(invitationPort.cancelExpiredPendingInvitationsByTargetAndEmail(
                InvitationType.WORKSPACE.name(),
                command.workspaceId(),
                email.address())
                .doOnNext(count -> logCleanupIfAny(
                    count,
                    command.workspaceId(),
                    command.requesterId())))
            .then(workspaceInvitationHelper
                .checkNotAlreadyMemberByEmail(command.workspaceId(), email)
                .then(workspaceInvitationHelper.checkDuplicatePendingInvitation(
                    command.workspaceId(), email)))
            .then(Mono.fromCallable(ulidGeneratorPort::generate)
                .flatMap(id -> invitationPort.save(
                    Invitation.createWorkspaceInvitation(
                        id,
                        command.workspaceId(),
                        email.address(),
                        command.role(),
                        command.requesterId())))))
        .as(transactionalOperator::transactional);
  }

  private void logCleanupIfAny(
      long cancelledCount,
      String targetId,
      String userId) {
    if (cancelledCount > 0) {
      log.info(
          "Invitation cleanup: reason={}, source={}, targetType={}, targetId={}, userId={}, cancelledCount={}",
          CLEANUP_REASON_EXPIRED_PENDING,
          CLEANUP_SOURCE_CREATE_WORKSPACE_INVITATION,
          InvitationType.WORKSPACE.name(),
          targetId,
          userId,
          cancelledCount);
    }
  }

}
