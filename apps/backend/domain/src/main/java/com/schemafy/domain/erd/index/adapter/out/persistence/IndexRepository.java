package com.schemafy.domain.erd.index.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface IndexRepository extends ReactiveCrudRepository<IndexEntity, String> {

  Flux<IndexEntity> findByTableId(String tableId);

  Mono<Void> deleteByTableId(String tableId);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_indexes i
        WHERE i.table_id = :tableId
          AND i.name = :name
      )
      """)
  Mono<Boolean> existsByTableIdAndName(String tableId, String name);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_indexes i
        WHERE i.table_id = :tableId
          AND i.name = :name
          AND i.id <> :indexId
      )
      """)
  Mono<Boolean> existsByTableIdAndNameExcludingId(
      String tableId,
      String name,
      String indexId);

}
