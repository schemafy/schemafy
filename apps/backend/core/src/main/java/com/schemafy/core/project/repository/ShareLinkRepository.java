package com.schemafy.core.project.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.ShareLink;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ShareLinkRepository
    extends ReactiveCrudRepository<ShareLink, String> {

  @Query("SELECT * FROM share_links WHERE id = :id AND deleted_at IS NULL")
  Mono<ShareLink> findByIdAndNotDeleted(String id);

  @Query("SELECT * FROM share_links WHERE token_hash = :tokenHash AND deleted_at IS NULL")
  Mono<ShareLink> findByTokenHashAndNotDeleted(byte[] tokenHash);

  @Query("SELECT * FROM share_links WHERE token_hash = :tokenHash AND is_revoked = false AND deleted_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)")
  Mono<ShareLink> findValidByTokenHash(byte[] tokenHash);

  @Query("SELECT * FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY created_at DESC")
  Flux<ShareLink> findByProjectIdAndNotDeleted(String projectId);

  @Query("SELECT * FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
  Flux<ShareLink> findByProjectIdAndNotDeleted(String projectId, int limit,
      int offset);

  @Query("SELECT COUNT(*) FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Long> countByProjectIdAndNotDeleted(String projectId);

  @Query("UPDATE share_links SET last_accessed_at = CURRENT_TIMESTAMP, access_count = access_count + 1 WHERE id = :id")
  Mono<Void> incrementAccessCount(String id);

  @Query("UPDATE share_links SET deleted_at = CURRENT_TIMESTAMP WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<Void> softDeleteByProjectId(String projectId);

}
