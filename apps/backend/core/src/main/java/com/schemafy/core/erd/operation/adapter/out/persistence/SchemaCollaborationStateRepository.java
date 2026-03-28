package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface SchemaCollaborationStateRepository
    extends ReactiveCrudRepository<SchemaCollaborationStateEntity, String> {

  @Modifying
  @Query("""
      UPDATE schema_collaboration_state
      SET current_revision = current_revision + 1,
          updated_at = CURRENT_TIMESTAMP,
          version = version + 1
      WHERE schema_id = :schemaId
      """)
  Mono<Long> incrementRevision(String schemaId);

}
