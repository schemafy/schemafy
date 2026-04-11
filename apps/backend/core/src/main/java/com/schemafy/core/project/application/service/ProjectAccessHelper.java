package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.ProjectDetail;
import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceMember;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.project.domain.exception.WorkspaceErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ProjectAccessHelper {

  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final WorkspaceMemberPort workspaceMemberPort;

  Mono<Project> findProjectById(String projectId) {
    return projectPort.findByIdAndNotDeleted(projectId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.NOT_FOUND)));
  }

  Mono<ProjectDetail> buildProjectDetail(Project project, String userId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(project.getId(), userId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.MEMBER_NOT_FOUND)))
        .map(member -> new ProjectDetail(project, member.getRole()));
  }

  Mono<ProjectMember> findProjectMember(String userId, String projectId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(new DomainException(
            ProjectErrorCode.MEMBER_NOT_FOUND)));
  }

  Mono<Void> softDeleteMember(ProjectMember member) {
    member.delete();
    return projectMemberPort.save(member).then();
  }

  Mono<Void> validateWorkspaceAdmin(String workspaceId, String userId) {
    return workspaceMemberPort
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .filter(WorkspaceMember::isAdmin)
        .switchIfEmpty(Mono.error(
            new DomainException(WorkspaceErrorCode.ACCESS_DENIED)))
        .then();
  }

  Mono<Void> validateWorkspaceMember(String workspaceId, String userId) {
    return workspaceMemberPort
        .findByWorkspaceIdAndUserIdAndNotDeleted(workspaceId, userId)
        .switchIfEmpty(Mono.error(new DomainException(
            WorkspaceErrorCode.ACCESS_DENIED)))
        .then();
  }

  Mono<Void> validateProjectMember(String projectId, String userId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .then();
  }

  Mono<Void> validateProjectAdmin(String projectId, String userId) {
    return projectMemberPort
        .findByProjectIdAndUserIdAndNotDeleted(projectId, userId)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ACCESS_DENIED)))
        .filter(ProjectMember::isAdmin)
        .switchIfEmpty(Mono.error(
            new DomainException(ProjectErrorCode.ADMIN_REQUIRED)))
        .then();
  }

  Mono<Void> validateWorkspaceAdminGuard(
      String projectId,
      ProjectMember target) {
    return findProjectById(projectId)
        .flatMap(project -> workspaceMemberPort
            .findByWorkspaceIdAndUserIdAndNotDeleted(project.getWorkspaceId(), target.getUserId())
            .filter(WorkspaceMember::isAdmin)
            .flatMap(admin -> Mono.error(new DomainException(ProjectErrorCode.WORKSPACE_ADMIN_PROJECT_ADMIN_PROTECTED)))
            .then());
  }

  void validateRoleChangePermission(
      ProjectMember requester,
      ProjectMember target,
      ProjectRole newRole) {
    ProjectRole requesterRole = requester.getRoleAsEnum();
    ProjectRole targetCurrentRole = target.getRoleAsEnum();

    if (targetCurrentRole == newRole) {
      throw new DomainException(ProjectErrorCode.SAME_ROLE_CHANGE_NOT_ALLOWED);
    }

    if (target.getUserId().equals(requester.getUserId())) {
      throw new DomainException(ProjectErrorCode.CANNOT_CHANGE_OWN_ROLE);
    }

    if (!requesterRole.isHigherOrEqualThan(newRole)) {
      throw new DomainException(ProjectErrorCode.CANNOT_ASSIGN_HIGHER_ROLE);
    }

    if (!requesterRole.isHigherOrEqualThan(targetCurrentRole)) {
      throw new DomainException(
          ProjectErrorCode.CANNOT_MODIFY_HIGHER_ROLE_MEMBER);
    }
  }

}
