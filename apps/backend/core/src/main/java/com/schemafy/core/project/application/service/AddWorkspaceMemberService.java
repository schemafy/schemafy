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
  private static final String CLEANUP_REASON_WORKSPACE_MEMBER_MATERIALIZED = "workspace_member_materialized";
  private static final String CLEANUP_SOURCE_ADD_WORKSPACE_MEMBER = "add_workspace_member";

  private final TransactionalOperator transactionalOperator;
  private final UlidGeneratorPort ulidGeneratorPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final InvitationPort invitationPort;
  private final WorkspaceAccessHelper workspaceAccessHelper;
  private final ProjectMembershipPropagationHelper projectMembershipPropagationHelper;

  @Override
  public Mono<WorkspaceMember> addWorkspaceMember(AddWorkspaceMemberCommand command) {
    // 권한 검증은 추후 어노테이션으로 분리할 예정이라 트랜잭션 진입 전 수행
    return Mono.fromSupplier(() -> Email.from(command.email()))
        .flatMap(email -> workspaceAccessHelper.validateAdminAccess(
            command.workspaceId(), command.requesterId())
            .then(workspaceAccessHelper.findUserByEmailOrThrow(email))
            .flatMap(targetUser -> Mono.defer(() -> doAddWorkspaceMember(
                command,
                email,
                targetUser.id())
                .as(transactionalOperator::transactional)))
            .onErrorResume(error -> {
              if (error instanceof DataIntegrityViolationException) {
                log.warn("Duplicate key constraint: workspaceId={}",
                    command.workspaceId());
                return Mono.error(new DomainException(
                    WorkspaceErrorCode.MEMBER_ALREADY_EXISTS));
              }
              return Mono.error(error);
            }));
  }

  private Mono<WorkspaceMember> doAddWorkspaceMember(
      AddWorkspaceMemberCommand command,
      Email email,
      String targetUserId) {
    return workspaceAccessHelper.findWorkspaceOrThrow(command.workspaceId())
        .then(createOrRestoreWorkspaceMember(command, targetUserId))
        .flatMap(savedMember -> projectMembershipPropagationHelper
            .syncProjectMembershipsForWorkspaceRole(
                command.workspaceId(),
                savedMember.getUserId(),
                savedMember.getRoleAsEnum())
            .then(invitationPort
                .cancelPendingProjectInvitationsByWorkspaceIdAndEmail(
                    command.workspaceId(), email.address())
                .doOnNext(count -> logCleanupIfAny(
                    count,
                    command.workspaceId(),
                    targetUserId)))
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
            .map(id -> WorkspaceMember.create(
                id,
                command.workspaceId(),
                userId,
                command.role()))
            .flatMap(workspaceMemberPort::save)));
  }

  private void logCleanupIfAny(
      long cancelledCount,
      String targetId,
      String userId) {
    if (cancelledCount > 0) {
      log.info(
          "Invitation cleanup: reason={}, source={}, targetType={}, targetId={}, userId={}, cancelledCount={}",
          CLEANUP_REASON_WORKSPACE_MEMBER_MATERIALIZED,
          CLEANUP_SOURCE_ADD_WORKSPACE_MEMBER,
          "PROJECT",
          targetId,
          userId,
          cancelledCount);
    }
  }

}
