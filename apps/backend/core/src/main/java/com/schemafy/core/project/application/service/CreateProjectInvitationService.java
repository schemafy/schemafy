package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger log = LoggerFactory.getLogger(
      CreateProjectInvitationService.class);
  private static final String CLEANUP_REASON_EXPIRED_PENDING = "expired_pending";
  private static final String CLEANUP_SOURCE_CREATE_PROJECT_INVITATION = "create_project_invitation";

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
            .flatMap(project -> Mono.defer(() -> doCreateProjectInvitation(
                command, email, project.getWorkspaceId())
                .as(transactionalOperator::transactional))));
  }

  private Mono<Invitation> doCreateProjectInvitation(
      CreateProjectInvitationCommand command,
      Email email,
      String workspaceId) {
    return projectAccessHelper.requireProjectWithinWorkspace(
        workspaceId, command.projectId())
        .flatMap(project -> invitationPort
            .cancelExpiredPendingInvitationsByTargetAndEmail(
                InvitationType.PROJECT.name(),
                command.projectId(),
                email.address())
            .doOnNext(count -> logCleanupIfAny(
                count,
                command.projectId(),
                command.requesterId()))
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

  private void logCleanupIfAny(
      long cancelledCount,
      String targetId,
      String userId) {
    if (cancelledCount > 0) {
      log.info(
          "Invitation cleanup: reason={}, source={}, targetType={}, targetId={}, userId={}, cancelledCount={}",
          CLEANUP_REASON_EXPIRED_PENDING,
          CLEANUP_SOURCE_CREATE_PROJECT_INVITATION,
          InvitationType.PROJECT.name(),
          targetId,
          userId,
          cancelledCount);
    }
  }

}
