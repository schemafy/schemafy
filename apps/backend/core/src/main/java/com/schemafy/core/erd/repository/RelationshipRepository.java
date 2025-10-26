package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;

import com.schemafy.core.erd.repository.entity.Relationship;

import reactor.core.publisher.Flux;

public interface RelationshipRepository
        extends ReactiveCrudRepository<Relationship, String> {

    @Query("SELECT * FROM db_relationships WHERE src_table_id = :tableId OR tgt_table_id = :tableId")
    public Flux<Relationship> findByTableId(String tableId);

}
