package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.domain.validator.ConstraintValidator;

import reactor.core.publisher.Mono;

@Service
public class ChangeConstraintColumnPositionService implements ChangeConstraintColumnPositionUseCase {

  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  public ChangeConstraintColumnPositionService(
      ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort,
      GetConstraintColumnByIdPort getConstraintColumnByIdPort,
      GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort) {
    this.changeConstraintColumnPositionPort = changeConstraintColumnPositionPort;
    this.getConstraintColumnByIdPort = getConstraintColumnByIdPort;
    this.getConstraintColumnsByConstraintIdPort = getConstraintColumnsByConstraintIdPort;
  }

  @Override
  public Mono<Void> changeConstraintColumnPosition(ChangeConstraintColumnPositionCommand command) {
    return Mono.defer(() -> {
      ConstraintValidator.validatePosition(command.seqNo());
      return getConstraintColumnByIdPort.findConstraintColumnById(command.constraintColumnId())
          .switchIfEmpty(Mono.error(new RuntimeException("Constraint column not found")))
          .flatMap(constraintColumn -> getConstraintColumnsByConstraintIdPort
              .findConstraintColumnsByConstraintId(constraintColumn.constraintId())
              .defaultIfEmpty(List.of())
              .flatMap(columns -> reorderColumns(
                  constraintColumn,
                  columns,
                  command.seqNo())));
    });
  }

  private Mono<Void> reorderColumns(
      ConstraintColumn constraintColumn,
      List<ConstraintColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new RuntimeException("Constraint column not found"));
    }
    if (nextPosition < 0 || nextPosition >= columns.size()) {
      return Mono.error(new ConstraintPositionInvalidException(
          "Constraint column position must be between 0 and %d".formatted(columns.size() - 1)));
    }

    List<ConstraintColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, constraintColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new RuntimeException("Constraint column not found"));
    }
    ConstraintColumn movingColumn = reordered.remove(currentIndex);
    reordered.add(nextPosition, movingColumn);

    List<ConstraintColumn> updated = new ArrayList<>(reordered.size());
    for (int index = 0; index < reordered.size(); index++) {
      ConstraintColumn column = reordered.get(index);
      updated.add(new ConstraintColumn(
          column.id(),
          column.constraintId(),
          column.columnId(),
          index));
    }

    return changeConstraintColumnPositionPort
        .changeConstraintColumnPositions(constraintColumn.constraintId(), updated);
  }

  private static int findIndex(List<ConstraintColumn> columns, String constraintColumnId) {
    for (int index = 0; index < columns.size(); index++) {
      if (equalsIgnoreCase(columns.get(index).id(), constraintColumnId)) {
        return index;
      }
    }
    return -1;
  }

  private static boolean equalsIgnoreCase(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

}
