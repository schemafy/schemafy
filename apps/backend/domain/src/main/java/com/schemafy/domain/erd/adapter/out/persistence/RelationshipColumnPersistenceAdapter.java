package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.domain.RelationshipColumn;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class RelationshipColumnPersistenceAdapter implements
    ChangeRelationshipColumnPositionPort,
    CreateRelationshipColumnPort,
    GetRelationshipColumnByIdPort,
    GetRelationshipColumnsByRelationshipIdPort,
    DeleteRelationshipColumnPort {

  private final RelationshipColumnRepository relationshipColumnRepository;
  private final RelationshipColumnMapper relationshipColumnMapper;

  RelationshipColumnPersistenceAdapter(
      RelationshipColumnRepository relationshipColumnRepository,
      RelationshipColumnMapper relationshipColumnMapper) {
    this.relationshipColumnRepository = relationshipColumnRepository;
    this.relationshipColumnMapper = relationshipColumnMapper;
  }

  @Override
  public Mono<RelationshipColumn> createRelationshipColumn(RelationshipColumn relationshipColumn) {
    RelationshipColumnEntity entity = Objects.requireNonNull(
        relationshipColumnMapper.toEntity(relationshipColumn));
    return relationshipColumnRepository.save(entity)
        .map(relationshipColumnMapper::toDomain);
  }

  @Override
  public Mono<RelationshipColumn> findRelationshipColumnById(String relationshipColumnId) {
    return relationshipColumnRepository.findByIdAndDeletedAtIsNull(relationshipColumnId)
        .map(relationshipColumnMapper::toDomain);
  }

  @Override
  public Mono<List<RelationshipColumn>> findRelationshipColumnsByRelationshipId(String relationshipId) {
    return relationshipColumnRepository.findByRelationshipIdAndDeletedAtIsNullOrderBySeqNo(relationshipId)
        .map(relationshipColumnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeRelationshipColumnPositions(
      String relationshipId,
      List<RelationshipColumn> columns) {
    if (columns == null || columns.isEmpty()) {
      return Mono.empty();
    }
    Map<String, Integer> positions = new HashMap<>(columns.size());
    for (RelationshipColumn column : columns) {
      positions.put(column.id(), column.seqNo());
    }
    return relationshipColumnRepository
        .findByRelationshipIdAndDeletedAtIsNullOrderBySeqNo(relationshipId)
        .map(entity -> {
          Integer seqNo = positions.get(entity.getId());
          if (seqNo != null) {
            entity.setSeqNo(seqNo);
          }
          return entity;
        })
        .collectList()
        .flatMap(entities -> relationshipColumnRepository.saveAll(entities).then());
  }

  @Override
  public Mono<Void> deleteRelationshipColumn(String relationshipColumnId) {
    return findActiveRelationshipColumnOrError(relationshipColumnId)
        .flatMap((@NonNull RelationshipColumnEntity relationshipColumnEntity) -> {
          relationshipColumnEntity.setDeletedAt(Instant.now());
          return relationshipColumnRepository.save(relationshipColumnEntity);
        })
        .then();
  }

  private Mono<RelationshipColumnEntity> findActiveRelationshipColumnOrError(
      String relationshipColumnId) {
    return relationshipColumnRepository.findByIdAndDeletedAtIsNull(relationshipColumnId)
        .switchIfEmpty(Mono.error(new RuntimeException("Relationship column not found")));
  }
}
