package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.Email;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateWorkspaceInvitationService
    implements CreateWorkspaceInvitationUseCase {

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
            .flatMap(workspace -> workspaceInvitationHelper
                .checkNotAlreadyMemberByEmail(command.workspaceId(), email)
                .then(workspaceInvitationHelper.checkDuplicatePendingInvitation(
                    command.workspaceId(), email))
                .thenReturn(workspace))
            .flatMap(workspace -> Mono.fromCallable(ulidGeneratorPort::generate)
                .flatMap(id -> invitationPort.save(
                    Invitation.createWorkspaceInvitation(
                        id,
                        command.workspaceId(),
                        email.address(),
                        command.role(),
                        command.requesterId())))))
        .as(transactionalOperator::transactional);
  }

}
