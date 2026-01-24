package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ConstraintRepository extends ReactiveCrudRepository<ConstraintEntity, String> {

  Flux<ConstraintEntity> findByTableId(String tableId);

  Mono<Void> deleteByTableId(String tableId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_constraints c
        JOIN db_tables t ON t.id = c.table_id
        WHERE t.schema_id = :schemaId
          AND c.name = :name
      )
      """)
  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_constraints c
        JOIN db_tables t ON t.id = c.table_id
        WHERE t.schema_id = :schemaId
          AND c.name = :name
          AND c.id <> :constraintId
      )
      """)
  Mono<Boolean> existsBySchemaIdAndNameExcludingId(
      String schemaId,
      String name,
      String constraintId);

}
