package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.IndexColumn;

import reactor.core.publisher.Flux;

public interface IndexColumnRepository
        extends ReactiveCrudRepository<IndexColumn, String> {

    public Flux<IndexColumn> findByindexId(String indexId);

}
