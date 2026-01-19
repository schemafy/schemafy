package com.schemafy.domain.erd.adapter.out.persistence;

import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ReactiveCreateSchema;
import com.schemafy.domain.erd.application.port.out.ReactiveSchemaExistsPort;
import com.schemafy.domain.erd.domain.Schema;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class ReactiveSchemaPersistenceAdapter implements ReactiveCreateSchema, ReactiveSchemaExistsPort {

  private final ReactiveSchemaRepository reactiveSchemaRepository;
  private final SchemaMapper schemaMapper;

  public Mono<Schema> findActiveSchemaById(String id) {
    return reactiveSchemaRepository.findByIdAndDeletedAtIsNull(id)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Mono<Schema> createSchema(Schema schema) {
    SchemaEntity entity = Objects.requireNonNull(schemaMapper.toEntity(schema));
    return reactiveSchemaRepository.save(entity)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Mono<Boolean> existsActiveByProjectIdAndName(String projectId, String name) {
    return reactiveSchemaRepository.existsActiveByProjectIdAndName(projectId, name);
  }

}
