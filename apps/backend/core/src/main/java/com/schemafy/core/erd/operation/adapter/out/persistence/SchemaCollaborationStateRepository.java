package com.schemafy.core.erd.operation.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface SchemaCollaborationStateRepository
    extends ReactiveCrudRepository<SchemaCollaborationStateEntity, String> {

  @Query("""
      SELECT schema_id, project_id, current_revision, created_at, updated_at
      FROM schema_collaboration_state
      WHERE schema_id = :schemaId
      FOR UPDATE
      """)
  Mono<SchemaCollaborationStateEntity> findByIdForUpdate(String schemaId);

  @Modifying
  @Query("""
      UPDATE schema_collaboration_state
      SET current_revision = current_revision + 1,
          updated_at = CURRENT_TIMESTAMP
      WHERE schema_id = :schemaId
      """)
  Mono<Long> incrementRevision(String schemaId);

  @Modifying
  @Query("""
      UPDATE schema_collaboration_state
      SET current_revision = current_revision + 1,
          updated_at = CURRENT_TIMESTAMP
      WHERE schema_id = :schemaId
        AND current_revision = :expectedRevision
      """)
  Mono<Long> incrementRevisionIfCurrentRevision(String schemaId, long expectedRevision);

}
