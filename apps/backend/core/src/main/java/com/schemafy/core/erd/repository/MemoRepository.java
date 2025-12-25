package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.Memo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MemoRepository extends ReactiveCrudRepository<Memo, String> {

  Mono<Memo> findByIdAndDeletedAtIsNull(String id);

  Flux<Memo> findBySchemaIdAndDeletedAtIsNull(String schemaId);

}
