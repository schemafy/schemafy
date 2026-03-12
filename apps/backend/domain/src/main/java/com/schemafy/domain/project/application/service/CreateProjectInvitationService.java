package com.schemafy.domain.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.project.application.port.in.CreateProjectInvitationCommand;
import com.schemafy.domain.project.application.port.in.CreateProjectInvitationUseCase;
import com.schemafy.domain.project.application.port.out.InvitationPort;
import com.schemafy.domain.project.domain.Invitation;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

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
  public Mono<Invitation> createProjectInvitation(
      CreateProjectInvitationCommand command) {
    return projectInvitationHelper.validateProjectAdmin(command.projectId(),
        command.requesterId())
        .then(projectInvitationHelper.findProjectOrThrow(command.projectId()))
        .flatMap(project -> projectInvitationHelper
            .checkNotAlreadyProjectMemberByEmail(command.projectId(),
                command.email())
            .then(projectInvitationHelper.checkDuplicatePendingInvitation(
                command.projectId(), command.email()))
            .thenReturn(project))
        .flatMap(project -> Mono.fromCallable(ulidGeneratorPort::generate)
            .flatMap(id -> invitationPort.save(
                Invitation.createProjectInvitation(
                    id,
                    command.projectId(),
                    project.getWorkspaceId(),
                    command.email(),
                    command.role(),
                    command.requesterId()))))
        .as(transactionalOperator::transactional);
  }

}
