package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface DbVendorRepository extends ReactiveCrudRepository<DbVendorEntity, Integer> {

  @Query("""
      SELECT *
      FROM db_vendors
      WHERE deleted_at IS NULL
      ORDER BY name, version, id
      """)
  Flux<DbVendorEntity> findAllActive();

  @Query("SELECT * FROM db_vendors WHERE id = :id AND deleted_at IS NULL")
  Mono<DbVendorEntity> findActiveById(Integer id);

}
