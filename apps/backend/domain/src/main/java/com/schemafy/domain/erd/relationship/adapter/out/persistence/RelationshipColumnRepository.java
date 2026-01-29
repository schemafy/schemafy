package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface RelationshipColumnRepository extends ReactiveCrudRepository<RelationshipColumnEntity, String> {

  Flux<RelationshipColumnEntity> findByRelationshipIdOrderBySeqNo(String relationshipId);

  @Query("SELECT * FROM db_relationship_columns WHERE pk_column_id = :columnId OR fk_column_id = :columnId")
  Flux<RelationshipColumnEntity> findByColumnId(String columnId);

  Mono<Void> deleteByRelationshipId(String relationshipId);

  @Query("DELETE FROM db_relationship_columns WHERE pk_column_id = :columnId OR fk_column_id = :columnId")
  Mono<Void> deleteByColumnId(String columnId);

}
