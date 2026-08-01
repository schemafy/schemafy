package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.domain.WorkspaceMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceMemberRepository
    extends ReactiveCrudRepository<WorkspaceMember, String> {

  Mono<WorkspaceMember> findByWorkspaceIdAndUserIdAndDeletedAtIsNull(
      String workspaceId, String userId);

  @Query("""
      SELECT * FROM workspace_members
      WHERE workspace_id = :workspaceId
        AND deleted_at IS NULL
      ORDER BY created_at ASC
      LIMIT :limit OFFSET :offset
      """)
  Flux<WorkspaceMember> findByWorkspaceIdAndNotDeleted(String workspaceId,
      int limit, int offset);

  @Query("""
      SELECT
        wm.user_id AS user_id,
        u.name AS user_name,
        u.email AS user_email,
        wm.role AS role,
        wm.created_at AS joined_at
      FROM workspace_members wm
      INNER JOIN users u ON u.id = wm.user_id
      WHERE wm.workspace_id = :workspaceId
        AND wm.deleted_at IS NULL
        AND (
            LOWER(u.name) LIKE :pattern ESCAPE '!'
            OR LOWER(u.email) LIKE :pattern ESCAPE '!'
        )
      ORDER BY wm.created_at ASC, wm.id ASC
      LIMIT :limit OFFSET :offset
      """)
  Flux<MemberSearchResult> searchMemberResultsByWorkspaceIdAndUser(
      String workspaceId, String pattern, int limit, int offset);

  Flux<WorkspaceMember> findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtAsc(
      String workspaceId);

  Mono<Long> countByWorkspaceIdAndDeletedAtIsNull(String workspaceId);

  @Query("""
      SELECT COUNT(*) FROM workspace_members wm
      INNER JOIN users u ON u.id = wm.user_id
      WHERE wm.workspace_id = :workspaceId
        AND wm.deleted_at IS NULL
        AND (
          LOWER(u.name) LIKE :pattern ESCAPE '!'
          OR LOWER(u.email) LIKE :pattern ESCAPE '!'
        )
      """)
  Mono<Long> countByWorkspaceIdAndUser(String workspaceId, String pattern);

  @Query("""
      UPDATE workspace_members
      SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE workspace_id = :workspaceId
        AND deleted_at IS NULL
      """)
  Mono<Void> softDeleteByWorkspaceId(String workspaceId);

  Mono<Boolean> existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(String workspaceId,
      String userId);

  Mono<Long> countByWorkspaceIdAndRoleAndDeletedAtIsNull(String workspaceId,
      String role);

  Mono<WorkspaceMember> findFirstByWorkspaceIdAndUserIdOrderByCreatedAtDesc(
      String workspaceId,
      String userId);

}
