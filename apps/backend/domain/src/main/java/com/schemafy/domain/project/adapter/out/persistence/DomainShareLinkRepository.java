package com.schemafy.domain.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.domain.project.domain.ShareLink;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DomainShareLinkRepository
    extends ReactiveCrudRepository<ShareLink, String> {

  @Query("SELECT * FROM share_links WHERE code = :code AND deleted_at IS NULL")
  Mono<ShareLink> findByCodeAndNotDeleted(String code);

  @Query("UPDATE share_links SET last_accessed_at = CURRENT_TIMESTAMP, access_count = access_count + 1 WHERE id = :id")
  Mono<Void> incrementAccessCount(String id);

  @Query("SELECT * FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<ShareLink> findByProjectIdAndNotDeleted(String projectId, int limit,
      int offset);

  @Query("SELECT COUNT(*) FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  @Query("SELECT * FROM share_links WHERE id = :id AND project_id = :projectId AND deleted_at IS NULL")
  Mono<ShareLink> findByIdAndProjectIdAndNotDeleted(String id,
      String projectId);

  @Query("""
      UPDATE share_links
      SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE project_id = :projectId
        AND deleted_at IS NULL
      """)
  Mono<Long> softDeleteByProjectId(String projectId);

}
