package com.schemafy.core.project.application.access;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AccessVerifier {

  private final WorkspaceMemberPort workspaceMemberPort;
  private final ProjectMemberPort projectMemberPort;

  public Mono<Void> requireProjectAccess(
      String projectId,
      String requesterId,
      ProjectRole requiredRole) {
    return projectMemberPort.findByProjectIdAndUserIdAndNotDeleted(projectId, requesterId)
        .switchIfEmpty(Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .flatMap(member -> verifyProjectRole(member, requiredRole));
  }

  public Mono<Void> requireWorkspaceAccess(
      String workspaceId,
      String requesterId,
      WorkspaceRole requiredRole) {
    return workspaceMemberPort.findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, requesterId)
        .switchIfEmpty(Mono.error(new DomainException(WorkspaceErrorCode.ACCESS_DENIED)))
        .flatMap(member -> verifyWorkspaceRole(member, requiredRole));
  }

  private Mono<Void> verifyProjectRole(ProjectMember member, ProjectRole requiredRole) {
    ProjectRole currentRole = member.getRoleAsEnum();
    if (currentRole.isHigherOrEqualThan(requiredRole)) {
      return Mono.empty();
    }
    if (requiredRole == ProjectRole.ADMIN) {
      return Mono.error(new DomainException(ProjectErrorCode.ADMIN_REQUIRED));
    }
    return Mono.error(new DomainException(ProjectErrorCode.ACCESS_DENIED));
  }

  private Mono<Void> verifyWorkspaceRole(WorkspaceMember member, WorkspaceRole requiredRole) {
    WorkspaceRole currentRole = member.getRoleAsEnum();
    if (currentRole.isHigherOrEqualThan(requiredRole)) {
      return Mono.empty();
    }
    if (requiredRole == WorkspaceRole.ADMIN) {
      return Mono.error(new DomainException(WorkspaceErrorCode.ADMIN_REQUIRED));
    }
    return Mono.error(new DomainException(WorkspaceErrorCode.ACCESS_DENIED));
  }

}
