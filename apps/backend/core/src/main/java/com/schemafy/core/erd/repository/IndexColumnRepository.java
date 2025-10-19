package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.IndexColumn;

public interface IndexColumnRepository
        extends ReactiveCrudRepository<IndexColumn, String> {

}
