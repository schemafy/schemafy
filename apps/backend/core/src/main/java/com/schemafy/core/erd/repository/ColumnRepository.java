package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Column;

public interface ColumnRepository
        extends ReactiveCrudRepository<Column, String> {

}
