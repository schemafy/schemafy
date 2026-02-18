package com.schemafy.domain.erd.constraint.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteConstraintService implements DeleteConstraintUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteConstraintPort deleteConstraintPort;
  private final DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<MutationResult<Void>> deleteConstraint(DeleteConstraintCommand command) {
    String constraintId = command.constraintId();
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(new ConstraintNotExistException("Constraint not found")))
        .flatMap(constraint -> {
          Set<String> affectedTableIds = new HashSet<>();
          affectedTableIds.add(constraint.tableId());
          if (constraint.kind() == ConstraintKind.PRIMARY_KEY) {
            return cascadeDeleteFkColumns(
                constraint.tableId(),
                constraintId,
                affectedTableIds)
                .then(deleteConstraintColumnsPort.deleteByConstraintId(constraintId))
                .then(deleteConstraintPort.deleteConstraint(constraintId))
                .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
          }
          return deleteConstraintColumnsPort.deleteByConstraintId(constraintId)
              .then(deleteConstraintPort.deleteConstraint(constraintId))
              .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
        })
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> cascadeDeleteFkColumns(
      String pkTableId,
      String constraintId,
      Set<String> affectedTableIds) {
    return getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId)
        .defaultIfEmpty(List.of())
        .flatMap(constraintColumns -> {
          if (constraintColumns.isEmpty()) {
            return Mono.empty();
          }

          List<String> pkColumnIds = constraintColumns.stream()
              .map(ConstraintColumn::columnId)
              .toList();

          return Flux.fromIterable(pkColumnIds)
              .concatMap(pkColumnId -> pkCascadeHelper.cascadeRemovePkColumn(
                  pkTableId,
                  pkColumnId,
                  new HashSet<>(),
                  affectedTableIds))
              .then();
        });
  }

}
