package com.schemafy.core.project.application.service;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.WorkspaceDetail;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.application.port.out.WorkspacePort;
import com.schemafy.core.project.domain.Workspace;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;
import com.schemafy.core.user.application.port.out.FindUserByEmailPort;
import com.schemafy.core.user.domain.Email;
import com.schemafy.core.user.domain.User;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class WorkspaceAccessHelper {

  private final WorkspacePort workspacePort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final ProjectPort projectPort;
  private final FindUserByEmailPort findUserByEmailPort;

  Mono<User> findUserByEmailOrThrow(Email email) {
    return findUserByEmailPort.findUserByEmail(email.address())
        .switchIfEmpty(Mono.error(new DomainException(UserErrorCode.NOT_FOUND)));
  }

  Mono<WorkspaceDetail> buildWorkspaceDetail(Workspace workspace, String userId) {
    return Mono.zip(
        projectPort.countByWorkspaceIdAndNotDeleted(workspace.getId()),
        workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(
            workspace.getId(), userId)
            .switchIfEmpty(Mono.error(new DomainException(
                WorkspaceErrorCode.MEMBER_NOT_FOUND)))
            .map(WorkspaceMember::getRole))
        .map(tuple -> new WorkspaceDetail(
            workspace,
            tuple.getT1(),
            tuple.getT2()));
  }

  Mono<Void> validateMemberAccess(String workspaceId, String userId) {
    return workspaceMemberPort
        .existsByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .flatMap(exists -> {
          if (!exists) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.ACCESS_DENIED));
          }
          return Mono.empty();
        });
  }

  Mono<Void> validateAdminAccess(String workspaceId, String userId) {
    return findWorkspaceMember(userId, workspaceId)
        .flatMap(member -> {
          if (!member.isAdmin()) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.ADMIN_REQUIRED));
          }
          return Mono.empty();
        });
  }

  Mono<Workspace> findWorkspaceOrThrow(String workspaceId) {
    return workspacePort.findByIdAndNotDeleted(workspaceId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.NOT_FOUND)));
  }

  Mono<Workspace> requireWorkspaceForWrite(String workspaceId) {
    return workspacePort.findByIdAndNotDeletedForUpdate(workspaceId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.NOT_FOUND)));
  }

  Mono<WorkspaceMember> modifyMemberWithAdminGuard(
      String workspaceId,
      WorkspaceMember member,
      Consumer<WorkspaceMember> action) {
    if (!member.isAdmin()) {
      action.accept(member);
      return workspaceMemberPort.save(member);
    }

    return workspaceMemberPort
        .countByWorkspaceIdAndRoleAndNotDeleted(workspaceId,
            WorkspaceRole.ADMIN.name())
        .flatMap(count -> {
          if (count <= 1) {
            return Mono.error(new DomainException(
                WorkspaceErrorCode.LAST_ADMIN_CANNOT_LEAVE));
          }
          action.accept(member);
          return workspaceMemberPort.save(member);
        });
  }

  Mono<WorkspaceMember> findWorkspaceMember(String userId, String workspaceId) {
    return workspaceMemberPort
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.MEMBER_NOT_FOUND)));
  }

}
