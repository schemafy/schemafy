package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.application.port.in.ProjectSearchResult;
import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectRepository extends ReactiveCrudRepository<Project, String> {

  Mono<Project> findByIdAndDeletedAtIsNull(String id);

  Flux<Project> findByWorkspaceIdAndDeletedAtIsNull(String workspaceId);

  Flux<Project> findByWorkspaceId(String workspaceId);

  Mono<Long> countByWorkspaceIdAndDeletedAtIsNull(String workspaceId);

  @Query("""
      SELECT p.* FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
      ORDER BY p.created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Project> findByWorkspaceIdAndUserIdWithPaging(String workspaceId,
      String userId, int limit, int offset);

  @Query("""
      SELECT p.* FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
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
  Flux<Project> findSharedByUserIdWithPaging(String userId,
      int limit,
      int offset);

  @Query("""
      SELECT
        p.id AS id,
        p.workspace_id AS workspace_id,
        p.db_vendor_id AS db_vendor_id,
        p.name AS name,
        p.description AS description,
        pm.role AS requester_role,
        p.created_at AS created_at,
        p.updated_at AS updated_at
      FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND LOWER(p.name) LIKE :pattern ESCAPE '!'
      ORDER BY p.created_at DESC, p.id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProjectSearchResult> searchByWorkspaceIdAndUserId(
      String workspaceId,
      String userId,
      String pattern,
      int limit,
      int offset);

  @Query("""
      SELECT COUNT(*)
      FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
      WHERE p.workspace_id = :workspaceId
        AND pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND LOWER(p.name) LIKE :pattern ESCAPE '!'
      """)
  Mono<Long> countSearchByWorkspaceIdAndUserId(
      String workspaceId,
      String userId,
      String pattern);

  @Query("""
      SELECT
        p.id AS id,
        p.workspace_id AS workspace_id,
        p.db_vendor_id AS db_vendor_id,
        p.name AS name,
        p.description AS description,
        pm.role AS requester_role,
        p.created_at AS created_at,
        p.updated_at AS updated_at
      FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
      WHERE pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND LOWER(p.name) LIKE :pattern ESCAPE '!'
        AND NOT EXISTS (
          SELECT 1 FROM workspace_members wm
          WHERE wm.workspace_id = p.workspace_id
            AND wm.user_id = :userId
            AND wm.deleted_at IS NULL
        )
      ORDER BY p.created_at DESC, p.id DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<ProjectSearchResult> searchSharedByUserId(
      String userId,
      String pattern,
      int limit,
      int offset);

  @Query("""
      SELECT COUNT(*)
      FROM projects p
      INNER JOIN project_members pm ON p.id = pm.project_id
      WHERE pm.user_id = :userId
        AND pm.deleted_at IS NULL
        AND p.deleted_at IS NULL
        AND LOWER(p.name) LIKE :pattern ESCAPE '!'
        AND NOT EXISTS (
          SELECT 1 FROM workspace_members wm
          WHERE wm.workspace_id = p.workspace_id
            AND wm.user_id = :userId
            AND wm.deleted_at IS NULL
        )
      """)
  Mono<Long> countSearchSharedByUserId(String userId, String pattern);

}
