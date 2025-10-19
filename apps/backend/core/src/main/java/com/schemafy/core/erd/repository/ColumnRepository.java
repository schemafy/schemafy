package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Column;

import reactor.core.publisher.Flux;

public interface ColumnRepository
        extends ReactiveCrudRepository<Column, String> {

    public Flux<Column> findByTableId(String tableId);

}
