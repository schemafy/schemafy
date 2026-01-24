package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.application.port.out.CreateRelationshipPort;
import com.schemafy.domain.erd.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.domain.Relationship;
import com.schemafy.domain.erd.domain.type.Cardinality;
import com.schemafy.domain.erd.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class RelationshipPersistenceAdapter implements
    CreateRelationshipPort,
    GetRelationshipByIdPort,
    GetRelationshipsBySchemaIdPort,
    ChangeRelationshipNamePort,
    ChangeRelationshipKindPort,
    ChangeRelationshipCardinalityPort,
    ChangeRelationshipExtraPort,
    DeleteRelationshipPort,
    RelationshipExistsPort {

  private final RelationshipRepository relationshipRepository;
  private final RelationshipMapper relationshipMapper;

  RelationshipPersistenceAdapter(
      RelationshipRepository relationshipRepository,
      RelationshipMapper relationshipMapper) {
    this.relationshipRepository = relationshipRepository;
    this.relationshipMapper = relationshipMapper;
  }

  @Override
  public Mono<Relationship> createRelationship(Relationship relationship) {
    RelationshipEntity entity = Objects.requireNonNull(relationshipMapper.toEntity(relationship));
    return relationshipRepository.save(entity)
        .map(relationshipMapper::toDomain);
  }

  @Override
  public Mono<Relationship> findRelationshipById(String relationshipId) {
    return relationshipRepository.findByIdAndDeletedAtIsNull(relationshipId)
        .map(relationshipMapper::toDomain);
  }

  @Override
  public Mono<List<Relationship>> findRelationshipsBySchemaId(String schemaId) {
    return relationshipRepository.findBySchemaId(schemaId)
        .map(relationshipMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeRelationshipName(String relationshipId, String newName) {
    return findActiveRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setName(newName);
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeRelationshipKind(String relationshipId, RelationshipKind kind) {
    return findActiveRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setKind(kind.name());
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeRelationshipCardinality(
      String relationshipId,
      Cardinality cardinality) {
    return findActiveRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setCardinality(cardinality.name());
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeRelationshipExtra(String relationshipId, String extra) {
    return findActiveRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setExtra(extra);
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteRelationship(String relationshipId) {
    return findActiveRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setDeletedAt(Instant.now());
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Boolean> existsByFkTableIdAndName(String fkTableId, String name) {
    return relationshipRepository.existsByFkTableIdAndName(fkTableId, name);
  }

  @Override
  public Mono<Boolean> existsByFkTableIdAndNameExcludingId(
      String fkTableId,
      String name,
      String relationshipId) {
    return relationshipRepository.existsByFkTableIdAndNameExcludingId(
        fkTableId,
        name,
        relationshipId);
  }

  private Mono<RelationshipEntity> findActiveRelationshipOrError(String relationshipId) {
    return relationshipRepository.findByIdAndDeletedAtIsNull(relationshipId)
        .switchIfEmpty(Mono.error(new RuntimeException("Relationship not found")));
  }
}
