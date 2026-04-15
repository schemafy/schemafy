package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.core.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
import com.schemafy.core.project.domain.Invitation;
import com.schemafy.core.project.domain.InvitationType;
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
  private final ProjectAccessHelper projectAccessHelper;

  @Override
  public Mono<Invitation> createProjectInvitation(
      CreateProjectInvitationCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> projectInvitationHelper.validateProjectAdmin(
            command.projectId(), command.requesterId())
            .then(projectAccessHelper.findProjectById(command.projectId()))
            .flatMap(project -> Mono.defer(() -> createProjectInvitationWithinWriteScope(
                command, email, project.getWorkspaceId())
                .as(transactionalOperator::transactional))));
  }

  private Mono<Invitation> createProjectInvitationWithinWriteScope(
      CreateProjectInvitationCommand command,
      Email email,
      String workspaceId) {
    return projectAccessHelper.requireProjectWithinWorkspaceForWrite(
        workspaceId, command.projectId())
        .flatMap(project -> invitationPort
            .cancelExpiredPendingInvitationsByTargetAndEmail(
                InvitationType.PROJECT.name(),
                command.projectId(),
                email.address())
            .then(projectInvitationHelper
                .checkNotAlreadyProjectMemberByEmail(command.projectId(), email)
                .then(projectInvitationHelper.checkDuplicatePendingInvitation(
                    command.projectId(), email))
                .thenReturn(project)))
        .flatMap(project -> Mono.fromCallable(ulidGeneratorPort::generate)
            .flatMap(id -> invitationPort.save(
                Invitation.createProjectInvitation(
                    id,
                    command.projectId(),
                    project.getWorkspaceId(),
                    email.address(),
                    command.role(),
                    command.requesterId()))));
  }

}
