package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Relationship;

public interface RelationshipRepository
        extends ReactiveCrudRepository<Relationship, String> {

}
