package com.schemafy.domain.erd.memo.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface MemoRepository extends ReactiveCrudRepository<MemoEntity, String> {

  Mono<MemoEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<MemoEntity> findBySchemaIdAndDeletedAtIsNull(String schemaId);

}
