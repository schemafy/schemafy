package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface TableRepository extends ReactiveCrudRepository<TableEntity, String> {

  Mono<TableEntity> findByIdAndDeletedAtIsNull(String id);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_tables
        WHERE schema_id = :schemaId
          AND name = :name
          AND deleted_at IS NULL
      )
      """)
  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

}
