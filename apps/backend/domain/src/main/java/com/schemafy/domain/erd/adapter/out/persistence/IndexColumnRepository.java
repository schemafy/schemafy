package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface IndexColumnRepository extends ReactiveCrudRepository<IndexColumnEntity, String> {

  Mono<IndexColumnEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<IndexColumnEntity> findByIndexIdAndDeletedAtIsNullOrderBySeqNo(String indexId);
}
