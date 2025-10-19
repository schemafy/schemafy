package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Relationship;

import reactor.core.publisher.Flux;

public interface RelationshipRepository
        extends ReactiveCrudRepository<Relationship, String> {

    public Flux<Relationship> findByTableId(String tableId);

}
