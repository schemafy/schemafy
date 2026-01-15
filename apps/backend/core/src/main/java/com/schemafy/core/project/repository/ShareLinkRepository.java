package com.schemafy.core.project.repository;

import java.time.Instant;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.project.repository.entity.ShareLink;

import reactor.core.publisher.Mono;

@Repository
public interface ShareLinkRepository
    extends ReactiveCrudRepository<ShareLink, String> {

  @Query("SELECT * FROM share_links WHERE code = :code AND is_revoked = false AND deleted_at IS NULL AND (expires_at IS NULL OR expires_at > :currentTime)")
  Mono<ShareLink> findValidLinkByCode(String code, Instant currentTime);

  @Query("UPDATE share_links SET last_accessed_at = CURRENT_TIMESTAMP, access_count = access_count + 1 WHERE id = :id")
  Mono<Void> incrementAccessCount(String id);

}
