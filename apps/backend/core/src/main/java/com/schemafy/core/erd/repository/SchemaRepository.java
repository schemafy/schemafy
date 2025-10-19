package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.Schema;

@Repository
public interface SchemaRepository
        extends ReactiveCrudRepository<Schema, String> {

}
