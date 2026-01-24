package com.schemafy.domain.erd.schema.adapter.out.persistence;

import java.time.Instant;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.schema.application.port.out.ChangeSchemaNamePort;
import com.schemafy.domain.erd.schema.application.port.out.CreateSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.application.port.out.SchemaExistsPort;
import com.schemafy.domain.erd.schema.domain.Schema;

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
    return findActiveSchemaOrError(schemaId)
        .flatMap((@NonNull SchemaEntity schemaEntity) -> {
          schemaEntity.setName(newName);
          return schemaRepository.save(schemaEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteSchema(String schemaId) {
    return findActiveSchemaOrError(schemaId)
        .flatMap((@NonNull SchemaEntity schemaEntity) -> {
          schemaEntity.setDeletedAt(Instant.now());
          return schemaRepository.save(schemaEntity);
        })
        .then();
  }

  private Mono<SchemaEntity> findActiveSchemaOrError(String schemaId) {
    return schemaRepository.findByIdAndDeletedAtIsNull(schemaId)
        .switchIfEmpty(Mono.error(new RuntimeException("Schema not found")));
  }

}
