package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RemoveConstraintColumnService implements RemoveConstraintColumnUseCase {

  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final DeleteConstraintPort deleteConstraintPort;
  private final TransactionalOperator transactionalOperator;
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<MutationResult<Void>> removeConstraintColumn(RemoveConstraintColumnCommand command) {
    return getConstraintColumnByIdPort
        .findConstraintColumnById(command.constraintColumnId())
        .switchIfEmpty(Mono.error(new ConstraintColumnNotExistException(
            "Constraint column not found: " + command.constraintColumnId())))
        .flatMap(constraintColumn -> getConstraintByIdPort
            .findConstraintById(constraintColumn.constraintId())
            .switchIfEmpty(Mono.error(new ConstraintNotExistException(
                "Constraint not found: " + constraintColumn.constraintId())))
            .flatMap(constraint -> {
              Set<String> affectedTableIds = new HashSet<>();
              affectedTableIds.add(constraint.tableId());
              return deleteConstraintColumnPort.deleteConstraintColumn(constraintColumn.id())
                  .then(handlePkConstraintColumnRemoval(
                      constraint,
                      constraintColumn.columnId(),
                      affectedTableIds))
                  .then(reorderOrDeleteConstraint(constraint.id()))
                  .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)));
            }))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> handlePkConstraintColumnRemoval(
      Constraint constraint,
      String columnId,
      Set<String> affectedTableIds) {
    if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
      return Mono.empty();
    }

    return pkCascadeHelper.cascadeRemovePkColumn(
        constraint.tableId(), columnId, new HashSet<>(), affectedTableIds);
  }

  private Mono<Void> reorderOrDeleteConstraint(String constraintId) {
    return getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteConstraintPort.deleteConstraint(constraintId);
          }
          List<ConstraintColumn> reordered = new ArrayList<>(columns.size());
          for (int index = 0; index < columns.size(); index++) {
            ConstraintColumn column = columns.get(index);
            reordered.add(new ConstraintColumn(
                column.id(),
                column.constraintId(),
                column.columnId(),
                index));
          }
          return changeConstraintColumnPositionPort
              .changeConstraintColumnPositions(constraintId, reordered);
        });
  }

}
