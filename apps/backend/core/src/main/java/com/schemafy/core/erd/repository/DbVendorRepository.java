package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.DbVendor;

import reactor.core.publisher.Mono;

@Repository
public interface DbVendorRepository
    extends ReactiveCrudRepository<DbVendor, String> {

  Mono<DbVendor> findByNameAndVersion(String name, String version);

}
