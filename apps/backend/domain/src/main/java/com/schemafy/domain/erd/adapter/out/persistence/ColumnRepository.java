package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ColumnRepository extends ReactiveCrudRepository<ColumnEntity, String> {

  Mono<ColumnEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<ColumnEntity> findByTableIdAndDeletedAtIsNullOrderBySeqNo(String tableId);

}
