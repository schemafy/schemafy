package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

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
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<Void> removeConstraintColumn(RemoveConstraintColumnCommand command) {
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new ConstraintNotExistException(
            "Constraint not found: " + command.constraintId())))
        .flatMap(constraint -> getConstraintColumnByIdPort
            .findConstraintColumnById(command.constraintColumnId())
            .switchIfEmpty(Mono.error(new ConstraintColumnNotExistException(
                "Constraint column not found: " + command.constraintColumnId())))
            .flatMap(constraintColumn -> {
              if (!constraintColumn.constraintId().equalsIgnoreCase(constraint.id())) {
                return Mono.error(new ConstraintColumnNotExistException(
                    "Constraint column does not belong to the constraint"));
              }
              return deleteConstraintColumnPort.deleteConstraintColumn(constraintColumn.id())
                  .then(handlePkConstraintColumnRemoval(constraint, constraintColumn.columnId()))
                  .then(reorderOrDeleteConstraint(constraint.id()));
            }));
  }

  private Mono<Void> handlePkConstraintColumnRemoval(Constraint constraint, String columnId) {
    if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
      return Mono.empty();
    }

    return pkCascadeHelper.cascadeRemovePkColumn(
        constraint.tableId(), columnId, new HashSet<>());
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
