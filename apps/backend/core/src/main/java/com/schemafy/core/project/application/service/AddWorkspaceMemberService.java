package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;
import com.schemafy.core.user.domain.Email;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class AddWorkspaceMemberService implements AddWorkspaceMemberUseCase {

  private static final Logger log = LoggerFactory.getLogger(
      AddWorkspaceMemberService.class);

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<WorkspaceMember> addWorkspaceMember(AddWorkspaceMemberCommand command) {
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> workspaceAccessHelper.validateAdminAccess(
            command.workspaceId(), command.requesterId())
            .then(workspaceAccessHelper.findUserByEmailOrThrow(email))
            .flatMap(targetUser -> workspaceMemberPort
                .findLatestByWorkspaceIdAndUserId(command.workspaceId(),
                    targetUser.id())
                .flatMap(existing -> {
                  if (!existing.isDeleted()) {
                    log.warn("Member already exists and is active: memberId={}",
                        existing.getId());
                    return Mono.error(new DomainException(
                        WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
                  }

                  existing.restore();
                  existing.updateRole(command.role());
                  return workspaceMemberPort.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> Mono
                    .fromCallable(ulidGeneratorPort::generate)
                    .flatMap(id -> workspaceMemberPort
                        .save(WorkspaceMember.create(id, command.workspaceId(),
                            targetUser.id(), command.role()))))))
            .flatMap(savedMember -> projectMembershipPropagationHelper
                .propagateToExistingProjects(
                    command.workspaceId(),
                    savedMember.getUserId(),
                    savedMember.getRoleAsEnum())
                .thenReturn(savedMember))
            .onErrorResume(error -> {
              if (error instanceof DataIntegrityViolationException) {
                log.warn("Duplicate key constraint: workspaceId={}, email={}",
                    command.workspaceId(), email.address());
                return Mono.error(new DomainException(
                    WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
              }
              return Mono.error(error);
            }))
        .as(transactionalOperator::transactional);
  }

}
