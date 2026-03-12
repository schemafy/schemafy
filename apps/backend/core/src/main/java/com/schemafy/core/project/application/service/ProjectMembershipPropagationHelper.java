package com.schemafy.core.project.application.service;

import org.springframework.dao.DataIntegrityViolationException;
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

  private static final Logger log = LoggerFactory.getLogger(
      ProjectMembershipPropagationHelper.class);

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
        .flatMap(wsMember -> projectMemberPort
            .existsByProjectIdAndUserIdAndNotDeleted(projectId,
                wsMember.getUserId())
            .flatMap(exists -> {
              if (exists) {
                return Mono.empty();
              }
              return Mono.fromCallable(ulidGeneratorPort::generate)
                  .flatMap(id -> {
                    ProjectRole projectRole = wsMember.getRoleAsEnum()
                        .toProjectRole();
                    ProjectMember newMember = ProjectMember.create(
                        id,
                        projectId,
                        wsMember.getUserId(),
                        projectRole);
                    return projectMemberPort.save(newMember).then();
                  });
            })
            .onErrorResume(DataIntegrityViolationException.class, error -> {
              log.warn("Duplicate key on auto-add user {} to project {}: {}",
                  wsMember.getUserId(), projectId, error.getMessage());
              return Mono.empty();
            }))
        .then();
  }

  Mono<Void> updateRoleInAllProjects(
      String workspaceId,
      String userId,
      WorkspaceRole workspaceRole) {
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

  Mono<Void> propagateToExistingProjects(
      String workspaceId,
      String userId,
      WorkspaceRole workspaceRole) {
    ProjectRole projectRole = workspaceRole.toProjectRole();

    return projectPort.findByWorkspaceIdAndNotDeleted(workspaceId)
        .concatMap(project -> projectMemberPort
            .findLatestByProjectIdAndUserId(project.getId(), userId)
            .flatMap(existing -> {
              if (!existing.isDeleted()) {
                return Mono.just(existing);
              }
              existing.restore();
              existing.updateRole(projectRole);
              return projectMemberPort.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> Mono
                .fromCallable(ulidGeneratorPort::generate)
                .flatMap(id -> projectMemberPort
                    .save(ProjectMember.create(id, project.getId(), userId,
                        projectRole)))))
            .then())
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
