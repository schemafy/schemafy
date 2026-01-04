package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Column;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ColumnRepository
    extends ReactiveCrudRepository<Column, String> {

  public Mono<Column> findByIdAndDeletedAtIsNull(String id);

  public Flux<Column> findByTableIdAndDeletedAtIsNull(String tableId);

}
