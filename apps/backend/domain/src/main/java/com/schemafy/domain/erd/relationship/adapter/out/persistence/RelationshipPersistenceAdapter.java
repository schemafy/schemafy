package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.relationship.application.port.out.CascadeDeleteRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipCardinalityPort;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipExtraPort;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByPkTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class RelationshipPersistenceAdapter implements
    CreateRelationshipPort,
    GetRelationshipByIdPort,
    GetRelationshipsBySchemaIdPort,
    GetRelationshipsByTableIdPort,
    GetRelationshipsByPkTableIdPort,
    ChangeRelationshipNamePort,
    ChangeRelationshipKindPort,
    ChangeRelationshipCardinalityPort,
    ChangeRelationshipExtraPort,
    DeleteRelationshipPort,
    RelationshipExistsPort,
    CascadeDeleteRelationshipsByTableIdPort {

  private final RelationshipRepository relationshipRepository;
  private final RelationshipColumnRepository relationshipColumnRepository;
  private final RelationshipMapper relationshipMapper;

  RelationshipPersistenceAdapter(
      RelationshipRepository relationshipRepository,
      RelationshipColumnRepository relationshipColumnRepository,
      RelationshipMapper relationshipMapper) {
    this.relationshipRepository = relationshipRepository;
    this.relationshipColumnRepository = relationshipColumnRepository;
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
    return relationshipRepository.findById(relationshipId)
        .map(relationshipMapper::toDomain);
  }

  @Override
  public Mono<List<Relationship>> findRelationshipsBySchemaId(String schemaId) {
    return relationshipRepository.findBySchemaId(schemaId)
        .map(relationshipMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<List<Relationship>> findRelationshipsByTableId(String tableId) {
    return relationshipRepository.findByTableId(tableId)
        .map(relationshipMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<List<Relationship>> findRelationshipsByPkTableId(String pkTableId) {
    return relationshipRepository.findByPkTableId(pkTableId)
        .map(relationshipMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeRelationshipName(String relationshipId, String newName) {
    return findRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setName(newName);
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeRelationshipKind(String relationshipId, RelationshipKind kind) {
    return findRelationshipOrError(relationshipId)
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
    return findRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setCardinality(cardinality.name());
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeRelationshipExtra(String relationshipId, String extra) {
    return findRelationshipOrError(relationshipId)
        .flatMap((@NonNull RelationshipEntity relationshipEntity) -> {
          relationshipEntity.setExtra(extra);
          return relationshipRepository.save(relationshipEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteRelationship(String relationshipId) {
    return relationshipRepository.deleteById(relationshipId);
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

  @Override
  public Mono<Void> cascadeDeleteByTableId(String tableId) {
    return relationshipRepository.findByTableId(tableId)
        .flatMap(
            relationship -> relationshipColumnRepository.deleteByRelationshipId(relationship.getId()))
        .then(relationshipRepository.deleteByTableId(tableId));
  }

  private Mono<RelationshipEntity> findRelationshipOrError(String relationshipId) {
    return relationshipRepository.findById(relationshipId)
        .switchIfEmpty(Mono.error(
            new RelationshipNotExistException("Relationship not found: " + relationshipId)));
  }

}
