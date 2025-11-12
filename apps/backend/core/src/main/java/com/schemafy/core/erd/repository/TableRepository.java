package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TableRepository extends ReactiveCrudRepository<Table, String> {

    public Flux<Table> findBySchemaIdAndDeletedAtIsNull(String schemaId);

    public Mono<Table> findByIdAndDeletedAtIsNull(String id);

}
