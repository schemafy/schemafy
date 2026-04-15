package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberCommand;
import com.schemafy.core.project.application.port.in.AddWorkspaceMemberUseCase;
import com.schemafy.core.project.application.port.out.InvitationPort;
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
  private final InvitationPort invitationPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<WorkspaceMember> addWorkspaceMember(AddWorkspaceMemberCommand command) {
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 lock보다 먼저 수행
    // commit 시점 권한까지 엄밀히 보장하려면 lock 이후 재검증 필요
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> workspaceAccessHelper.validateAdminAccess(
            command.workspaceId(), command.requesterId())
            .then(workspaceAccessHelper.findUserByEmailOrThrow(email))
            .flatMap(targetUser -> Mono.defer(() -> addWorkspaceMemberWithinWriteScope(command, email, targetUser.id())
                .as(transactionalOperator::transactional)))
            .onErrorResume(error -> {
              if (error instanceof DataIntegrityViolationException) {
                log.warn("Duplicate key constraint: workspaceId={}, email={}",
                    command.workspaceId(), email.address());
                return Mono.error(new DomainException(
                    WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
              }
              return Mono.error(error);
            }));
  }

  private Mono<WorkspaceMember> addWorkspaceMemberWithinWriteScope(
      AddWorkspaceMemberCommand command,
      Email email,
      String targetUserId) {
    return workspaceAccessHelper.requireWorkspaceForWrite(command.workspaceId())
        .then(createOrRestoreWorkspaceMember(command, targetUserId))
        .flatMap(savedMember -> projectMembershipPropagationHelper
            .syncProjectMembershipsForWorkspaceRole(
                command.workspaceId(),
                savedMember.getUserId(),
                savedMember.getRoleAsEnum())
            .then(invitationPort
                .cancelPendingProjectInvitationsByWorkspaceIdAndEmail(
                    command.workspaceId(), email.address()))
            .thenReturn(savedMember));
  }

  private Mono<WorkspaceMember> createOrRestoreWorkspaceMember(
      AddWorkspaceMemberCommand command,
      String userId) {
    return workspaceMemberPort.findByWorkspaceIdAndUserId(
        command.workspaceId(), userId)
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
            .map(id -> WorkspaceMember.create(id, command.workspaceId(), userId, command.role()))
            .flatMap(workspaceMemberPort::save)));
  }

}
