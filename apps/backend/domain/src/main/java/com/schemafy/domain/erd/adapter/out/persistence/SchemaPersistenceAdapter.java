package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.domain.Schema;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class SchemaPersistenceAdapter implements
    CreateSchemaPort,
    GetSchemaByIdPort,
    SchemaExistsPort,
    ChangeSchemaNamePort,
    DeleteSchemaPort {

  private final SchemaRepository schemaRepository;
  private final SchemaMapper schemaMapper;

  @Override
  public Mono<Schema> createSchema(Schema schema) {
    SchemaEntity entity = Objects.requireNonNull(schemaMapper.toEntity(schema));
    return schemaRepository.save(entity)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Mono<Schema> findSchemaById(String schemaId) {
    return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Mono<Boolean> existsActiveByProjectIdAndName(String projectId, String name) {
    return schemaRepository.existsActiveByProjectIdAndName(projectId, name);
  }

  @Override
  public Mono<Void> changeSchemaName(String schemaId, String newName) {
    return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
        .flatMap(schemaEntity -> {
          schemaEntity.setName(newName);
          return schemaRepository.save(schemaEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteSchema(String schemaId) {
    return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
        .flatMap(schemaEntity -> {
          schemaEntity.setDeletedAt(Instant.now());
          return schemaRepository.save(schemaEntity);
        })
        .then();
  }

}
