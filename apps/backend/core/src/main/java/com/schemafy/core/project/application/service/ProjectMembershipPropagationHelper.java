package com.schemafy.core.project.application.service;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemafy.core.project.application.port.out.ProjectMemberPort;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.WorkspaceMemberPort;
import com.schemafy.core.project.domain.ProjectMember;
import com.schemafy.core.project.domain.ProjectRole;
import com.schemafy.core.project.domain.WorkspaceRole;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class ProjectMembershipPropagationHelper {

  private static final Logger log = LoggerFactory.getLogger(ProjectMembershipPropagationHelper.class);
  private static final int PROJECT_MEMBER_UPSERT_BATCH_SIZE = 100;

  private final ProjectPort projectPort;
  private final ProjectMemberPort projectMemberPort;
  private final WorkspaceMemberPort workspaceMemberPort;
  private final UlidGeneratorPort ulidGeneratorPort;

  Mono<Void> propagateWorkspaceMembersToProject(
      String projectId,
      String workspaceId,
      String creatorUserId) {
    return workspaceMemberPort.findAllByWorkspaceIdAndNotDeleted(workspaceId)
        .filter(wsMember -> !wsMember.getUserId().equals(creatorUserId))
        .concatMap(wsMember -> Mono.fromCallable(ulidGeneratorPort::generate)
            .map(id -> ProjectMember.create(
                id,
                projectId,
                wsMember.getUserId(),
                wsMember.getRoleAsEnum().toProjectRole())))
        .buffer(PROJECT_MEMBER_UPSERT_BATCH_SIZE)
        .concatMap(members -> projectMemberPort.upsertAllForProject(projectId,
            members))
        .then();
  }

  Mono<Void> updateActiveProjectMembershipRoles(
      String workspaceId,
      String userId,
      WorkspaceRole workspaceRole) {
    // 현재 활성 상태인 프로젝트 멤버십의 역할만 변경한다.
    ProjectRole projectRole = workspaceRole.toProjectRole();
    return projectMemberPort
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .filter(member -> shouldPropagateWorkspaceRole(member, projectRole))
        .concatMap(member -> {
          member.updateRole(projectRole);
          return projectMemberPort.save(member);
        })
        .count()
        .doOnNext(count -> {
          if (count > 0) {
            log.info(
                "Propagated role to {} project memberships: workspace={}, user={}, role={}",
                count, workspaceId, userId, projectRole);
          }
        })
        .then();
  }

  Mono<Void> removeFromAllProjects(String workspaceId, String userId) {
    return projectMemberPort.softDeleteByWorkspaceIdAndUserId(workspaceId,
        userId)
        .doOnNext(count -> {
          if (count > 0) {
            log.info(
                "Cascade removed {} project memberships: workspace={}, user={}",
                count, workspaceId, userId);
          }
        })
        .then();
  }

  Mono<Void> syncProjectMembershipsForWorkspaceRole(
      String workspaceId,
      String userId,
      WorkspaceRole workspaceRole) {
    // 워크스페이스의 모든 프로젝트를 순회하며 멤버십을 승격, 복원, 생성해 상태를 맞춘다.
    ProjectRole projectRole = workspaceRole.toProjectRole();

    return projectPort.findByWorkspaceIdAndNotDeleted(workspaceId)
        .concatMap(project -> Mono.fromCallable(ulidGeneratorPort::generate)
            .map(id -> ProjectMember.create(id, project.getId(), userId, projectRole)))
        .buffer(PROJECT_MEMBER_UPSERT_BATCH_SIZE)
        .concatMap(members -> projectMemberPort.upsertAllForUser(userId,
            members))
        .then();
  }

  private boolean shouldPropagateWorkspaceRole(
      ProjectMember member,
      ProjectRole targetRole) {
    ProjectRole currentRole = member.getRoleAsEnum();

    if (currentRole == targetRole) {
      return false;
    }

    if (currentRole == ProjectRole.EDITOR && targetRole == ProjectRole.VIEWER) {
      return false;
    }

    return true;
  }

}
