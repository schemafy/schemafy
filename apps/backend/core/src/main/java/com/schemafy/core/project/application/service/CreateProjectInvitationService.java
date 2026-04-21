package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.Email;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class CreateProjectInvitationService implements CreateProjectInvitationUseCase {

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final InvitationPort invitationPort;
  private final ProjectInvitationHelper projectInvitationHelper;

  @Override
  @RequireProjectAccess(role = ProjectRole.ADMIN)
  public Mono<Invitation> createProjectInvitation(
      CreateProjectInvitationCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> projectInvitationHelper
            .findProjectOrThrow(command.projectId())
            .flatMap(project -> projectInvitationHelper
                .checkNotAlreadyProjectMemberByEmail(command.projectId(), email)
                .then(projectInvitationHelper.checkDuplicatePendingInvitation(
                    command.projectId(), email))
                .thenReturn(project))
            .flatMap(project -> Mono.fromCallable(ulidGeneratorPort::generate)
                .flatMap(id -> invitationPort.save(
                    Invitation.createProjectInvitation(
                        id,
                        command.projectId(),
                        project.getWorkspaceId(),
                        email.address(),
                        command.role(),
                        command.requesterId())))))
        .as(transactionalOperator::transactional);
  }

}
