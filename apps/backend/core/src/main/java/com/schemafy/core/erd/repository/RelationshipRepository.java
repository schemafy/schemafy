package com.schemafy.core.erd.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Relationship;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RelationshipRepository
    extends ReactiveCrudRepository<Relationship, String> {

  public Mono<Relationship> findByIdAndDeletedAtIsNull(String id);

  @Query("SELECT * FROM db_relationships WHERE deleted_at IS NULL AND (fk_table_id = :tableId OR pk_table_id = :tableId)")
  public Flux<Relationship> findByTableIdAndDeletedAtIsNull(String tableId);

}
