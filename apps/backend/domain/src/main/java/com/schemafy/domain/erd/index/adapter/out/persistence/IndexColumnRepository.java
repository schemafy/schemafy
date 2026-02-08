package com.schemafy.domain.erd.index.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface IndexColumnRepository extends ReactiveCrudRepository<IndexColumnEntity, String> {

  Flux<IndexColumnEntity> findByIndexIdOrderBySeqNo(String indexId);

  Flux<IndexColumnEntity> findByColumnId(String columnId);

  Mono<Void> deleteByIndexId(String indexId);

  Mono<Void> deleteByColumnId(String columnId);

}
