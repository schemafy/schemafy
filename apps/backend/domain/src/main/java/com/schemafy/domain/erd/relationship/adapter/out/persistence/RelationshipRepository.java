package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface RelationshipRepository extends ReactiveCrudRepository<RelationshipEntity, String> {

  Flux<RelationshipEntity> findByFkTableId(String fkTableId);

  @Query("SELECT * FROM db_relationships WHERE pk_table_id = :tableId OR fk_table_id = :tableId")
  Flux<RelationshipEntity> findByTableId(String tableId);

  @Query("DELETE FROM db_relationships WHERE pk_table_id = :tableId OR fk_table_id = :tableId")
  Mono<Void> deleteByTableId(String tableId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_relationships r
        WHERE r.fk_table_id = :fkTableId
          AND r.name = :name
      )
      """)
  Mono<Boolean> existsByFkTableIdAndName(String fkTableId, String name);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_relationships r
        WHERE r.fk_table_id = :fkTableId
          AND r.name = :name
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
      """)
  Flux<RelationshipEntity> findBySchemaId(String schemaId);

}
