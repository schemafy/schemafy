package com.schemafy.domain.erd.column.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ColumnRepository extends ReactiveCrudRepository<ColumnEntity, String> {

  Flux<ColumnEntity> findByTableIdOrderBySeqNo(String tableId);

  Mono<Void> deleteByTableId(String tableId);

}
