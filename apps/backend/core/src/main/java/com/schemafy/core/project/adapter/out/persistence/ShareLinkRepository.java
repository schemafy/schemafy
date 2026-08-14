package com.schemafy.core.project.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.project.domain.ShareLink;

import reactor.core.publisher.Mono;

public interface ShareLinkRepository
    extends ReactiveCrudRepository<ShareLink, String> {

  @Query("SELECT * FROM share_links WHERE id = :id AND deleted_at IS NULL")
  Mono<ShareLink> findByIdAndNotDeleted(String id);

  @Query("SELECT * FROM share_links WHERE project_id = :projectId AND deleted_at IS NULL")
  Mono<ShareLink> findByProjectIdAndNotDeleted(String projectId);

  @Query("""
      DELETE FROM share_links WHERE project_id = :projectId
      """)
  Mono<Void> deleteByProjectId(String projectId);

}
