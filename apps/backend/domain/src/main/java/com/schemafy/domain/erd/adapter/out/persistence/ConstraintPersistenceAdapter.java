package com.schemafy.domain.erd.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.erd.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.application.port.out.CreateConstraintPort;
import com.schemafy.domain.erd.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.domain.Constraint;

import reactor.core.publisher.Mono;

@PersistenceAdapter
class ConstraintPersistenceAdapter implements
    CreateConstraintPort,
    GetConstraintByIdPort,
    GetConstraintsByTableIdPort,
    ChangeConstraintNamePort,
    DeleteConstraintPort,
    ConstraintExistsPort {

  private final ConstraintRepository constraintRepository;
  private final ConstraintMapper constraintMapper;

  ConstraintPersistenceAdapter(
      ConstraintRepository constraintRepository,
      ConstraintMapper constraintMapper) {
    this.constraintRepository = constraintRepository;
    this.constraintMapper = constraintMapper;
  }

  @Override
  public Mono<Constraint> createConstraint(Constraint constraint) {
    ConstraintEntity entity = Objects.requireNonNull(constraintMapper.toEntity(constraint));
    return constraintRepository.save(entity)
        .map(constraintMapper::toDomain);
  }

  @Override
  public Mono<Constraint> findConstraintById(String constraintId) {
    return constraintRepository.findByIdAndDeletedAtIsNull(constraintId)
        .map(constraintMapper::toDomain);
  }

  @Override
  public Mono<List<Constraint>> findConstraintsByTableId(String tableId) {
    return constraintRepository.findByTableIdAndDeletedAtIsNull(tableId)
        .map(constraintMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeConstraintName(String constraintId, String newName) {
    return findActiveConstraintOrError(constraintId)
        .flatMap((@NonNull ConstraintEntity constraintEntity) -> {
          constraintEntity.setName(newName);
          return constraintRepository.save(constraintEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteConstraint(String constraintId) {
    return findActiveConstraintOrError(constraintId)
        .flatMap((@NonNull ConstraintEntity constraintEntity) -> {
          constraintEntity.setDeletedAt(Instant.now());
          return constraintRepository.save(constraintEntity);
        })
        .then();
  }

  @Override
  public Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name) {
    return constraintRepository.existsBySchemaIdAndName(schemaId, name);
  }

  @Override
  public Mono<Boolean> existsBySchemaIdAndNameExcludingId(
      String schemaId,
      String name,
      String constraintId) {
    return constraintRepository.existsBySchemaIdAndNameExcludingId(schemaId, name, constraintId);
  }

  private Mono<ConstraintEntity> findActiveConstraintOrError(String constraintId) {
    return constraintRepository.findByIdAndDeletedAtIsNull(constraintId)
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")));
  }
}
