package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.RelationshipColumn;

import reactor.core.publisher.Flux;

public interface RelationshipColumnRepository
        extends ReactiveCrudRepository<RelationshipColumn, String> {

    public Flux<RelationshipColumn> findByRelationshipId(String relationshipId);

}
