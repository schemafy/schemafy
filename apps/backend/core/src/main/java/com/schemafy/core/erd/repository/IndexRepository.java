package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Index;

import reactor.core.publisher.Flux;

public interface IndexRepository extends ReactiveCrudRepository<Index, String> {

    public Flux<Index> findByTableId(String tableId);

}
