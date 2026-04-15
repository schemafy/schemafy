package com.schemafy.core.project.application.port.out;

import com.schemafy.core.project.domain.WorkspaceMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceMemberPort {

  Mono<WorkspaceMember> save(WorkspaceMember workspaceMember);

  Mono<WorkspaceMember> findByWorkspaceIdAndUserIdAndNotDeleted(
      String workspaceId,
      String userId);

  Flux<WorkspaceMember> findByWorkspaceIdAndNotDeleted(
      String workspaceId,
      int limit,
      int offset);

  Flux<WorkspaceMember> findAllByWorkspaceIdAndNotDeleted(String workspaceId);

  Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);

  Mono<Void> softDeleteByWorkspaceId(String workspaceId);

  Mono<Boolean> existsByWorkspaceIdAndUserIdAndNotDeleted(
      String workspaceId,
      String userId);

  Mono<Long> countByWorkspaceIdAndRoleAndNotDeleted(
      String workspaceId,
      String role);

  Mono<WorkspaceMember> findByWorkspaceIdAndUserId(
      String workspaceId,
      String userId);

}
