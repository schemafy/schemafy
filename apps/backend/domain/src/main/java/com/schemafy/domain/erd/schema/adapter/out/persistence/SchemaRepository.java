package com.schemafy.domain.erd.schema.adapter.out.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

interface SchemaRepository extends ReactiveCrudRepository<SchemaEntity, String> {

  Mono<SchemaEntity> findByIdAndDeletedAtIsNull(String id);

  @Query("""
      SELECT EXISTS(
        SELECT 1 FROM db_schemas
        WHERE project_id = :projectId
          AND name = :name
          AND deleted_at IS NULL
      )
      """)
  Mono<Boolean> existsActiveByProjectIdAndName(String projectId, String name);

}
