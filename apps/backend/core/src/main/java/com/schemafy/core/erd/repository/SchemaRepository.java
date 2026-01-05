package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.Schema;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SchemaRepository
    extends ReactiveCrudRepository<Schema, String> {

  public Mono<Schema> findByIdAndDeletedAtIsNull(String id);

  public Flux<Schema> findByProjectIdAndDeletedAtIsNull(String projectId);

}
