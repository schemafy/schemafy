package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.ProjectMember;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
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

  @Query("UPDATE project_members SET deleted_at = CURRENT_TIMESTAMP WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Void> softDeleteByProjectId(String projectId);

  @Query("SELECT COUNT(*) FROM project_members WHERE project_id = :projectId AND role = :role AND deleted_at IS NULL")
  Mono<Long> countByProjectIdAndRoleAndNotDeleted(String projectId,
      String role);

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
      SELECT COUNT(*) FROM project_members pm
      INNER JOIN projects p ON pm.project_id = p.id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
      """)
  Mono<Long> countByWorkspaceIdAndUserId(String workspaceId, String userId);

}
