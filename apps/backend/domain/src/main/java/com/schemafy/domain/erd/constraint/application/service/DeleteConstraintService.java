package com.schemafy.domain.erd.constraint.application.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.DeleteConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.DeleteConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteConstraintService implements DeleteConstraintUseCase {

  private final DeleteConstraintPort deleteConstraintPort;
  private final DeleteConstraintColumnsByConstraintIdPort deleteConstraintColumnsPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<Void> deleteConstraint(DeleteConstraintCommand command) {
    String constraintId = command.constraintId();
    return getConstraintByIdPort.findConstraintById(constraintId)
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
        .flatMap(constraint -> {
          if (constraint.kind() == ConstraintKind.PRIMARY_KEY) {
            return cascadeDeleteFkColumns(constraint.tableId(), constraintId)
                .then(deleteConstraintColumnsPort.deleteByConstraintId(constraintId))
                .then(deleteConstraintPort.deleteConstraint(constraintId));
          }
          return deleteConstraintColumnsPort.deleteByConstraintId(constraintId)
              .then(deleteConstraintPort.deleteConstraint(constraintId));
        });
  }

  private Mono<Void> cascadeDeleteFkColumns(String pkTableId, String constraintId) {
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
                  pkTableId, pkColumnId, new HashSet<>()))
              .then();
        });
  }

}
