package com.schemafy.domain.erd.table.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface TableRepository extends ReactiveCrudRepository<TableEntity, String> {

  Flux<TableEntity> findBySchemaId(String schemaId);

  Mono<Void> deleteBySchemaId(String schemaId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_tables
        WHERE schema_id = :schemaId
          AND name = :name
      )
      """)
  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

}
