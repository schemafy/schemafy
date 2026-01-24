package com.schemafy.domain.erd.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.RemoveConstraintColumnCommand;
import com.schemafy.domain.erd.application.port.in.RemoveConstraintColumnUseCase;
import com.schemafy.domain.erd.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.application.port.out.DeleteConstraintColumnPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.domain.ConstraintColumn;

import reactor.core.publisher.Mono;

@Service
public class RemoveConstraintColumnService implements RemoveConstraintColumnUseCase {

  private final DeleteConstraintColumnPort deleteConstraintColumnPort;
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  public RemoveConstraintColumnService(
      DeleteConstraintColumnPort deleteConstraintColumnPort,
      ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetConstraintColumnByIdPort getConstraintColumnByIdPort,
      GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort) {
    this.deleteConstraintColumnPort = deleteConstraintColumnPort;
    this.changeConstraintColumnPositionPort = changeConstraintColumnPositionPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getConstraintColumnByIdPort = getConstraintColumnByIdPort;
    this.getConstraintColumnsByConstraintIdPort = getConstraintColumnsByConstraintIdPort;
  }

  @Override
  public Mono<Void> removeConstraintColumn(RemoveConstraintColumnCommand command) {
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
        .flatMap(constraint -> getConstraintColumnByIdPort
            .findConstraintColumnById(command.constraintColumnId())
            .switchIfEmpty(Mono.error(new RuntimeException("Constraint column not found")))
            .flatMap(constraintColumn -> {
              if (!constraintColumn.constraintId().equalsIgnoreCase(constraint.id())) {
                return Mono.error(new RuntimeException("Constraint column not found"));
              }
              return deleteConstraintColumnPort.deleteConstraintColumn(constraintColumn.id())
                  .then(reorderRemainingColumns(constraint.id()));
            }));
  }

  private Mono<Void> reorderRemainingColumns(String constraintId) {
    return getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(constraintId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return Mono.empty();
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
