package com.schemafy.domain.erd.constraint.adapter.out.persistence;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;

import com.schemafy.domain.common.PersistenceAdapter;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintExpressionPort;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PersistenceAdapter
@RequiredArgsConstructor
class ConstraintPersistenceAdapter implements
    CreateConstraintPort,
    GetConstraintByIdPort,
    GetConstraintsByTableIdPort,
    ChangeConstraintNamePort,
    ChangeConstraintExpressionPort,
    DeleteConstraintPort,
    ConstraintExistsPort {

  private final ConstraintRepository constraintRepository;
  private final ConstraintColumnRepository constraintColumnRepository;
  private final ConstraintMapper constraintMapper;

  @Override
  public Mono<Constraint> createConstraint(Constraint constraint) {
    ConstraintEntity entity = Objects.requireNonNull(constraintMapper.toEntity(constraint));
    return constraintRepository.save(entity)
        .map(constraintMapper::toDomain);
  }

  @Override
  public Mono<Constraint> findConstraintById(String constraintId) {
    return constraintRepository.findById(constraintId)
        .map(constraintMapper::toDomain);
  }

  @Override
  public Mono<List<Constraint>> findConstraintsByTableId(String tableId) {
    return constraintRepository.findByTableId(tableId)
        .map(constraintMapper::toDomain)
        .collectList();
  }

  @Override
  public Mono<Void> changeConstraintName(String constraintId, String newName) {
    return findConstraintOrError(constraintId)
        .flatMap((@NonNull ConstraintEntity constraintEntity) -> {
          constraintEntity.setName(newName);
          return constraintRepository.save(constraintEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> changeConstraintExpressions(String constraintId, String checkExpr, String defaultExpr) {
    return findConstraintOrError(constraintId)
        .flatMap((@NonNull ConstraintEntity constraintEntity) -> {
          constraintEntity.setCheckExpr(checkExpr);
          constraintEntity.setDefaultExpr(defaultExpr);
          return constraintRepository.save(constraintEntity);
        })
        .then();
  }

  @Override
  public Mono<Void> deleteConstraint(String constraintId) {
    return constraintRepository.deleteById(constraintId);
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

  private Mono<ConstraintEntity> findConstraintOrError(String constraintId) {
    return constraintRepository.findById(constraintId)
        .switchIfEmpty(Mono.error(
            new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found: " + constraintId)));
  }

}
