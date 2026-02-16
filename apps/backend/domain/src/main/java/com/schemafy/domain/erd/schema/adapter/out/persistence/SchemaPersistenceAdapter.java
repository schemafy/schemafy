package com.schemafy.domain.erd.schema.adapter.out.persistence;

import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemasByProjectIdPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.schema.domain.exception.SchemaNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class SchemaPersistenceAdapter implements
    CreateSchemaPort,
    GetSchemaByIdPort,
    GetSchemasByProjectIdPort,
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
    return schemaRepository.findById(schemaId)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Flux<Schema> findSchemasByProjectId(String projectId) {
    return schemaRepository.findByProjectId(projectId)
        .map(schemaMapper::toDomain);
  }

  @Override
  public Mono<Boolean> existsActiveByProjectIdAndName(String projectId, String name) {
    return schemaRepository.existsActiveByProjectIdAndName(projectId, name);
  }

  @Override
  public Mono<Void> changeSchemaName(String schemaId, String newName) {
    return findSchemaOrError(schemaId)
        .flatMap((@NonNull SchemaEntity schemaEntity) -> {
          schemaEntity.setName(newName);
          return schemaRepository.save(schemaEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteSchema(String schemaId) {
    return schemaRepository.deleteById(schemaId);
  }

  private Mono<SchemaEntity> findSchemaOrError(String schemaId) {
    return schemaRepository.findById(schemaId)
        .switchIfEmpty(Mono.error(new SchemaNotExistException("Schema not found: " + schemaId)));
  }

}
