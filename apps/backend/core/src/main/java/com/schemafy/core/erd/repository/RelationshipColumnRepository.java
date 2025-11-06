package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RelationshipColumnRepository
        extends ReactiveCrudRepository<RelationshipColumn, String> {

    public Mono<RelationshipColumn> findByIdAndDeletedAtIsNull(String id);

    public Flux<RelationshipColumn> findByRelationshipIdAndDeletedAtIsNull(String relationshipId);

}
