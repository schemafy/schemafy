package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ConstraintRepository extends ReactiveCrudRepository<ConstraintEntity, String> {

  Mono<ConstraintEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<ConstraintEntity> findByTableIdAndDeletedAtIsNull(String tableId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_constraints c
        JOIN db_tables t ON t.id = c.table_id
        WHERE t.schema_id = :schemaId
          AND c.name = :name
          AND t.deleted_at IS NULL
          AND c.deleted_at IS NULL
      )
      """)
  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_constraints c
        JOIN db_tables t ON t.id = c.table_id
        WHERE t.schema_id = :schemaId
          AND c.name = :name
          AND t.deleted_at IS NULL
          AND c.deleted_at IS NULL
          AND c.id <> :constraintId
      )
      """)
  Mono<Boolean> existsBySchemaIdAndNameExcludingId(
      String schemaId,
      String name,
      String constraintId);

}
