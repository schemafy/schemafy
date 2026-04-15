package com.schemafy.core.project.adapter.out.persistence;

import java.util.Collection;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.domain.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectMemberRepository
    extends ReactiveCrudRepository<ProjectMember, String> {

  @Query("SELECT * FROM project_members WHERE project_id = :projectId AND user_id = :userId AND deleted_at IS NULL")
  Mono<ProjectMember> findByProjectIdAndUserIdAndNotDeleted(String projectId,
      String userId);

  @Query("SELECT * FROM project_members WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY joined_at LIMIT :limit OFFSET :offset")
  Flux<ProjectMember> findByProjectIdAndNotDeleted(String projectId,
      int limit, int offset);

  @Query("SELECT COUNT(*) FROM project_members WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  @Query("SELECT EXISTS(SELECT 1 FROM project_members WHERE project_id = :projectId AND user_id = :userId AND deleted_at IS NULL)")
  Mono<Boolean> existsByProjectIdAndUserIdAndNotDeleted(String projectId,
      String userId);

  @Query("UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Void> softDeleteByProjectId(String projectId);

  @Query("""
      UPDATE project_members
      SET role = :role,
          deleted_at = NULL,
          updated_at = CURRENT_TIMESTAMP
      WHERE project_id = :projectId
        AND user_id IN (:userIds)
        AND deleted_at IS NOT NULL
      """)
  Mono<Long> restoreDeletedByProjectIdAndUserIds(String role, String projectId,
      Collection<String> userIds);

  @Query("""
      UPDATE project_members
      SET role = :role,
          deleted_at = NULL,
          updated_at = CURRENT_TIMESTAMP
      WHERE project_id IN (:projectIds)
        AND user_id = :userId
        AND deleted_at IS NOT NULL
      """)
  Mono<Long> restoreDeletedByProjectIdsAndUserId(String role,
      Collection<String> projectIds, String userId);

  @Query("""
      UPDATE project_members
      SET role = 'ADMIN',
          updated_at = CURRENT_TIMESTAMP
      WHERE project_id = :projectId
        AND user_id IN (:userIds)
        AND deleted_at IS NULL
        AND role <> 'ADMIN'
      """)
  Mono<Long> setAdminForActiveMembersByProjectIdAndUserIds(String projectId,
      Collection<String> userIds);

  @Query("""
      UPDATE project_members
      SET role = 'ADMIN',
          updated_at = CURRENT_TIMESTAMP
      WHERE project_id IN (:projectIds)
        AND user_id = :userId
        AND deleted_at IS NULL
        AND role <> 'ADMIN'
      """)
  Mono<Long> setAdminForActiveMembersByProjectIdsAndUserId(
      Collection<String> projectIds, String userId);

  @Query("SELECT COUNT(*) FROM project_members WHERE project_id = :projectId AND role = :role AND deleted_at IS NULL")
  Mono<Long> countByProjectIdAndRoleAndNotDeleted(String projectId,
      String role);

  @Query("SELECT * FROM project_members WHERE project_id = :projectId AND user_id = :userId ORDER BY created_at DESC LIMIT 1")
  Mono<ProjectMember> findLatestByProjectIdAndUserId(String projectId,
      String userId);

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
