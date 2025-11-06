package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.IndexColumn;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IndexColumnRepository
        extends ReactiveCrudRepository<IndexColumn, String> {

    public Mono<IndexColumn> findByIdAndDeletedAtIsNull(String id);

    public Flux<IndexColumn> findByIndexIdAndDeletedAtIsNull(String indexId);

}
