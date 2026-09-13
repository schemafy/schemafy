package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;

import com.schemafy.core.project.application.access.RequireWorkspaceAccess;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateWorkspaceInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.Email;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateWorkspaceInvitationService
    implements CreateWorkspaceInvitationUseCase {

  private final WorkspaceMutationGuard workspaceMutationGuard;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final InvitationPort invitationPort;
  private final WorkspaceInvitationHelper workspaceInvitationHelper;
  private final WorkspaceAccessHelper workspaceAccessHelper;

  @Override
  @RequireWorkspaceAccess(role = WorkspaceRole.ADMIN)
  public Mono<Invitation> createWorkspaceInvitation(
      CreateWorkspaceInvitationCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> workspaceMutationGuard.protectShared(
            command.workspaceId(), () -> workspaceAccessHelper
                .findWorkspaceAdminMember(command.requesterId(), command.workspaceId())
                .then(Mono.defer(() -> workspaceInvitationHelper
                    .findWorkspaceOrThrow(command.workspaceId())
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
                                command.requesterId()))))))));
  }

}
