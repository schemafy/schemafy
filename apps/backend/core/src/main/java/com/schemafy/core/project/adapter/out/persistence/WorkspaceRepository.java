package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.domain.Workspace;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkspaceRepository
    extends ReactiveCrudRepository<Workspace, String> {

  Mono<Workspace> findByIdAndDeletedAtIsNull(String id);

  @Lock(LockMode.PESSIMISTIC_READ)
  Mono<Workspace> findWithSharedLockByIdAndDeletedAtIsNull(String id);

  @Lock(LockMode.PESSIMISTIC_WRITE)
  Mono<Workspace> findWithExclusiveLockByIdAndDeletedAtIsNull(String id);

  @Modifying
  @Query("""
      UPDATE workspaces
      SET name = :name,
          description = :description,
          updated_at = CURRENT_TIMESTAMP
      WHERE id = :id
        AND deleted_at IS NULL
      """)
  Mono<Long> updateIfActive(String id, String name, String description);

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
