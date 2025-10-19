package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.schemafy.core.erd.repository.entity.Index;

public interface IndexRepository extends ReactiveCrudRepository<Index, String> {

}
