package com.schemafy.core.project.application.port.out;

import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectMemberPort {

  Mono<ProjectMember> save(ProjectMember projectMember);

  Mono<ProjectMember> findByProjectIdAndUserIdAndNotDeleted(
      String projectId,
      String userId);

  Flux<ProjectMember> findByProjectIdAndNotDeleted(
      String projectId,
      int limit,
      int offset);

  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  Mono<Boolean> existsByProjectIdAndUserIdAndNotDeleted(
      String projectId,
      String userId);

  Mono<Void> softDeleteByProjectId(String projectId);

  Mono<Long> countByProjectIdAndRoleAndNotDeleted(
      String projectId,
      String role);

  Mono<ProjectMember> findLatestByProjectIdAndUserId(
      String projectId,
      String userId);

  Flux<String> findRolesByWorkspaceIdAndUserIdWithPaging(
      String workspaceId,
      String userId,
      int limit,
      int offset);

  Flux<String> findSharedRolesByUserIdWithPaging(
      String userId,
      int limit,
      int offset);

  Mono<Long> countByWorkspaceIdAndUserId(String workspaceId, String userId);

  Mono<Long> softDeleteByWorkspaceIdAndUserId(String workspaceId,
      String userId);

  Flux<ProjectMember> findByWorkspaceIdAndUserId(String workspaceId,
      String userId);

  Mono<Long> countSharedByUserId(String userId);

}
