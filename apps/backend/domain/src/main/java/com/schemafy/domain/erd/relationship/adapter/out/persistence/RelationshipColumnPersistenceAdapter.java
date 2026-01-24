package com.schemafy.domain.erd.relationship.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipColumnPositionPort;
import com.schemafy.domain.erd.relationship.application.port.out.CreateRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByColumnIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.domain.erd.relationship.domain.RelationshipColumn;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipColumnNotExistException;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class RelationshipColumnPersistenceAdapter implements
    ChangeRelationshipColumnPositionPort,
    CreateRelationshipColumnPort,
    GetRelationshipColumnByIdPort,
    GetRelationshipColumnsByRelationshipIdPort,
    DeleteRelationshipColumnPort,
    DeleteRelationshipColumnsByRelationshipIdPort,
    DeleteRelationshipColumnsByColumnIdPort {

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
    return relationshipColumnRepository.findById(relationshipColumnId)
        .map(relationshipColumnMapper::toDomain);
  }

  @Override
  public Mono<List<RelationshipColumn>> findRelationshipColumnsByRelationshipId(String relationshipId) {
    return relationshipColumnRepository.findByRelationshipIdOrderBySeqNo(relationshipId)
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
        .findByRelationshipIdOrderBySeqNo(relationshipId)
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
    return relationshipColumnRepository.deleteById(relationshipColumnId);
  }

  @Override
  public Mono<Void> deleteByRelationshipId(String relationshipId) {
    return relationshipColumnRepository.deleteByRelationshipId(relationshipId);
  }

  @Override
  public Mono<Void> deleteByColumnId(String columnId) {
    return relationshipColumnRepository.deleteByColumnId(columnId);
  }

  private Mono<RelationshipColumnEntity> findRelationshipColumnOrError(
      String relationshipColumnId) {
    return relationshipColumnRepository.findById(relationshipColumnId)
        .switchIfEmpty(Mono.error(
            new RelationshipColumnNotExistException(
                "Relationship column not found: " + relationshipColumnId)));
  }

}
