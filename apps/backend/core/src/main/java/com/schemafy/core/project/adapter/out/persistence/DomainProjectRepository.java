package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.domain.Project;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DomainProjectRepository extends ReactiveCrudRepository<Project, String> {

  @Query("SELECT * FROM projects WHERE id = :id AND deleted_at IS NULL")
  Mono<Project> findByIdAndNotDeleted(String id);

  @Query("SELECT * FROM projects WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
  Flux<Project> findByWorkspaceIdAndNotDeleted(String workspaceId);

  @Query("SELECT * FROM projects WHERE workspace_id = :workspaceId")
  Flux<Project> findByWorkspaceId(String workspaceId);

  @Query("SELECT COUNT(*) FROM projects WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
  Mono<Long> countByWorkspaceIdAndNotDeleted(String workspaceId);

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

}
