package com.schemafy.core.project.application.port.out;

import java.util.Collection;

import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectMemberPort {

  Mono<ProjectMember> save(ProjectMember projectMember);

  /** 하나의 프로젝트에 여러 사용자의 프로젝트 멤버십을 맞춘다.
   * 프로젝트 생성 후 워크스페이스 멤버를 새 프로젝트로 전파할 때 사용한다. */
  Mono<Void> upsertAllForProject(String projectId,
      Collection<ProjectMember> projectMembers);

  /** 하나의 사용자를 여러 프로젝트의 멤버십으로 맞춘다.
   * 워크스페이스 멤버 추가, 초대 수락, 역할 변경을 프로젝트들에 전파할 때 사용한다. */
  Mono<Void> upsertAllForUser(String userId,
      Collection<ProjectMember> projectMembers);

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
