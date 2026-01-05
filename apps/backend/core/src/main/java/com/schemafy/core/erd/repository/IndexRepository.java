package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Index;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IndexRepository extends ReactiveCrudRepository<Index, String> {

  public Mono<Index> findByIdAndDeletedAtIsNull(String id);

  public Flux<Index> findByTableIdAndDeletedAtIsNull(String tableId);

}
