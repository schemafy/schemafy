package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class ConstraintColumnPersistenceAdapter implements
    CreateConstraintColumnPort,
    GetConstraintColumnByIdPort,
    GetConstraintColumnsByConstraintIdPort,
    DeleteConstraintColumnPort {

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
    return constraintColumnRepository.findByIdAndDeletedAtIsNull(constraintColumnId)
        .map(constraintColumnMapper::toDomain);
  }

  @Override
  public Mono<List<ConstraintColumn>> findConstraintColumnsByConstraintId(String constraintId) {
    return constraintColumnRepository.findByConstraintIdAndDeletedAtIsNullOrderBySeqNo(constraintId)
        .map(constraintColumnMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> deleteConstraintColumn(String constraintColumnId) {
    return findActiveConstraintColumnOrError(constraintColumnId)
        .flatMap((@NonNull ConstraintColumnEntity constraintColumnEntity) -> {
          constraintColumnEntity.setDeletedAt(Instant.now());
          return constraintColumnRepository.save(constraintColumnEntity);
        })
        .then();
  }

  private Mono<ConstraintColumnEntity> findActiveConstraintColumnOrError(String constraintColumnId) {
    return constraintColumnRepository.findByIdAndDeletedAtIsNull(constraintColumnId)
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint column not found")));
  }
}
