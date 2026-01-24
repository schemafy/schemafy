package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface RelationshipRepository extends ReactiveCrudRepository<RelationshipEntity, String> {

  Mono<RelationshipEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<RelationshipEntity> findByFkTableIdAndDeletedAtIsNull(String fkTableId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_relationships r
        WHERE r.fk_table_id = :fkTableId
          AND r.name = :name
          AND r.deleted_at IS NULL
      )
      """)
  Mono<Boolean> existsByFkTableIdAndName(String fkTableId, String name);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_relationships r
        WHERE r.fk_table_id = :fkTableId
          AND r.name = :name
          AND r.deleted_at IS NULL
          AND r.id <> :relationshipId
      )
      """)
  Mono<Boolean> existsByFkTableIdAndNameExcludingId(
      String fkTableId,
      String name,
      String relationshipId);

  @Query("""
      SELECT r.* FROM db_relationships r
      JOIN db_tables t ON t.id = r.fk_table_id
      WHERE t.schema_id = :schemaId
        AND r.deleted_at IS NULL
        AND t.deleted_at IS NULL
      """)
  Flux<RelationshipEntity> findBySchemaId(String schemaId);
}
