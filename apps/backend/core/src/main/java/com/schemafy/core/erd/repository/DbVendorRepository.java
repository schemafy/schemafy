package com.schemafy.core.erd.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.schemafy.core.erd.repository.entity.DbVendor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DbVendorRepository
    extends ReactiveCrudRepository<DbVendor, String> {

  public Mono<DbVendor> findByIdAndDeletedAtIsNull(String id);

  public Flux<DbVendor> findByDeletedAtIsNull();

  public Mono<DbVendor> findByNameAndVersionAndDeletedAtIsNull(String name,
      String version);

}
