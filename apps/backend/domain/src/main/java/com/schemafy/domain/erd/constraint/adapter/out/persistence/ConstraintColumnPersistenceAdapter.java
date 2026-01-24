package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByColumnIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class ConstraintColumnPersistenceAdapter implements
    ChangeConstraintColumnPositionPort,
    CreateConstraintColumnPort,
    GetConstraintColumnByIdPort,
    GetConstraintColumnsByConstraintIdPort,
    DeleteConstraintColumnPort,
    DeleteConstraintColumnsByConstraintIdPort,
    DeleteConstraintColumnsByColumnIdPort {

  private final ConstraintColumnRepository constraintColumnRepository;
  private final ConstraintColumnMapper constraintColumnMapper;

  ConstraintColumnPersistenceAdapter(
      ConstraintColumnRepository constraintColumnRepository,
      ConstraintColumnMapper constraintColumnMapper) {
    this.constraintColumnRepository = constraintColumnRepository;
    this.constraintColumnMapper = constraintColumnMapper;
  }

  @Override
  public Mono<ConstraintColumn> createConstraintColumn(ConstraintColumn constraintColumn) {
    ConstraintColumnEntity entity = Objects.requireNonNull(
        constraintColumnMapper.toEntity(constraintColumn));
    return constraintColumnRepository.save(entity)
        .map(constraintColumnMapper::toDomain);
  }

  @Override
  public Mono<ConstraintColumn> findConstraintColumnById(String constraintColumnId) {
    return constraintColumnRepository.findById(constraintColumnId)
        .map(constraintColumnMapper::toDomain);
  }

  @Override
  public Mono<List<ConstraintColumn>> findConstraintColumnsByConstraintId(String constraintId) {
    return constraintColumnRepository.findByConstraintIdOrderBySeqNo(constraintId)
        .map(constraintColumnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeConstraintColumnPositions(
      String constraintId,
      List<ConstraintColumn> columns) {
    if (columns == null || columns.isEmpty()) {
      return Mono.empty();
    }
    Map<String, Integer> positions = new HashMap<>(columns.size());
    for (ConstraintColumn column : columns) {
      positions.put(column.id(), column.seqNo());
    }
    return constraintColumnRepository.findByConstraintIdOrderBySeqNo(constraintId)
        .map(entity -> {
          Integer seqNo = positions.get(entity.getId());
          if (seqNo != null) {
            entity.setSeqNo(seqNo);
          }
          return entity;
        })
        .collectList()
        .flatMap(entities -> constraintColumnRepository.saveAll(entities).then());
  }

  @Override
  public Mono<Void> deleteConstraintColumn(String constraintColumnId) {
    return constraintColumnRepository.deleteById(constraintColumnId);
  }

  @Override
  public Mono<Void> deleteByConstraintId(String constraintId) {
    return constraintColumnRepository.deleteByConstraintId(constraintId);
  }

  @Override
  public Mono<Void> deleteByColumnId(String columnId) {
    return constraintColumnRepository.deleteByColumnId(columnId);
  }

  private Mono<ConstraintColumnEntity> findConstraintColumnOrError(String constraintColumnId) {
    return constraintColumnRepository.findById(constraintColumnId)
        .switchIfEmpty(Mono.error(
            new ConstraintColumnNotExistException(
                "Constraint column not found: " + constraintColumnId)));
  }

}
