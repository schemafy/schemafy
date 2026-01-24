package com.schemafy.domain.erd.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface RelationshipColumnRepository extends ReactiveCrudRepository<RelationshipColumnEntity, String> {

  Mono<RelationshipColumnEntity> findByIdAndDeletedAtIsNull(String id);

  Flux<RelationshipColumnEntity> findByRelationshipIdAndDeletedAtIsNullOrderBySeqNo(String relationshipId);
}
