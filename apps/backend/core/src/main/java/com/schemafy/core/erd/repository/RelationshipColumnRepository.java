package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.RelationshipColumn;

public interface RelationshipColumnRepository
        extends ReactiveCrudRepository<RelationshipColumn, String> {
}
