package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.application.port.in.MemberSearchResult;
import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectMemberRepository
    extends ReactiveCrudRepository<ProjectMember, String> {

  Mono<ProjectMember> findByProjectIdAndUserIdAndDeletedAtIsNull(String projectId,
      String userId);

  @Query("SELECT * FROM project_members WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY joined_at LIMIT :limit OFFSET :offset")
  Flux<ProjectMember> findByProjectIdAndNotDeleted(String projectId,
      int limit, int offset);

  @Query("""
      SELECT
        pm.user_id AS user_id,
        u.name AS user_name,
        u.email AS user_email,
        pm.role AS role,
        pm.joined_at AS joined_at
      FROM project_members pm
      INNER JOIN users u ON u.id = pm.user_id
      WHERE pm.project_id = :projectId
        AND pm.deleted_at IS NULL
        AND (
          LOWER(u.name) LIKE :pattern ESCAPE '!'
          OR LOWER(u.email) LIKE :pattern ESCAPE '!'
        )
      ORDER BY pm.joined_at ASC
      LIMIT :limit OFFSET :offset
      """)
  Flux<MemberSearchResult> searchMemberResultsByProjectIdAndUser(
      String projectId, String pattern, int limit, int offset);

  Mono<Long> countByProjectIdAndDeletedAtIsNull(String projectId);

  @Query("""
      SELECT COUNT(*) FROM project_members pm
      INNER JOIN users u ON u.id = pm.user_id
      WHERE pm.project_id = :projectId
        AND pm.deleted_at IS NULL
        AND (
          LOWER(u.name) LIKE :pattern ESCAPE '!'
          OR LOWER(u.email) LIKE :pattern ESCAPE '!'
        )
      """)
  Mono<Long> countByProjectIdAndUser(String projectId, String pattern);

  Mono<Boolean> existsByProjectIdAndUserIdAndDeletedAtIsNull(String projectId,
      String userId);

  @Query("UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Void> softDeleteByProjectId(String projectId);

  Mono<Long> countByProjectIdAndRoleAndDeletedAtIsNull(String projectId,
      String role);

  Mono<ProjectMember> findFirstByProjectIdAndUserIdOrderByCreatedAtDesc(
      String projectId, String userId);

  @Query("""
      SELECT pm.role FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
      ORDER BY p.created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<String> findRolesByWorkspaceIdAndUserIdWithPaging(String workspaceId,
      String userId, int limit, int offset);

  @Query("""
      SELECT pm.role FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND NOT EXISTS (
          SELECT 1 FROM workspace_members wm
          WHERE wm.workspace_id = p.workspace_id
            AND wm.user_id = :userId
            AND wm.deleted_at IS NULL
        )
      ORDER BY p.created_at DESC, p.id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<String> findSharedRolesByUserIdWithPaging(String userId, int limit,
      int offset);

  @Query("""
      SELECT COUNT(*) FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
      """)
  Mono<Long> countByWorkspaceIdAndUserId(String workspaceId, String userId);

  @Query("""
      UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE user_id = :userId
        AND project_id IN (
          SELECT id FROM projects
          WHERE workspace_id = :workspaceId AND deleted_at IS NULL
        )
        AND deleted_at IS NULL
      """)
  Mono<Long> softDeleteByWorkspaceIdAndUserId(String workspaceId, String userId);

  @Query("""
      SELECT pm.* FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
      """)
  Flux<ProjectMember> findByWorkspaceIdAndUserId(String workspaceId,
      String userId);

  @Query("""
      SELECT COUNT(*) FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND NOT EXISTS (
          SELECT 1 FROM workspace_members wm
          WHERE wm.workspace_id = p.workspace_id
            AND wm.user_id = :userId
            AND wm.deleted_at IS NULL
        )
      """)
  Mono<Long> countSharedByUserId(String userId);

}
