package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.repository.entity.Workspace;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceRepository
    extends ReactiveCrudRepository<Workspace, String> {

  @Query("""
      SELECT * FROM workspaces
      WHERE id = :id AND deleted_at IS NULL
      """)
  Mono<Workspace> findByIdAndNotDeleted(String id);

  @Query("""
      SELECT * FROM workspaces
      WHERE owner_id = :ownerId AND deleted_at IS NULL
      """)
  Mono<Workspace> findByOwnerIdAndNotDeleted(String ownerId);

  @Query("""
      UPDATE workspaces
      SET deleted_at = CURRENT_TIMESTAMP
      WHERE id = :id
      """)
  Mono<Void> softDelete(String id);

  @Query("""
      SELECT w.* FROM workspaces w
      INNER JOIN workspace_members wm ON w.id = wm.workspace_id
      WHERE wm.user_id = :userId
        AND wm.deleted_at IS NULL
        AND w.deleted_at IS NULL
      ORDER BY w.created_at DESC
      LIMIT :limit OFFSET :offset
      """)
  Flux<Workspace> findByUserIdWithPaging(String userId, int limit,
      int offset);

  @Query("""
      SELECT COUNT(*) FROM workspaces w
      INNER JOIN workspace_members wm ON w.id = wm.workspace_id
      WHERE wm.user_id = :userId
        AND wm.deleted_at IS NULL
        AND w.deleted_at IS NULL
      """)
  Mono<Long> countByUserId(String userId);

}
