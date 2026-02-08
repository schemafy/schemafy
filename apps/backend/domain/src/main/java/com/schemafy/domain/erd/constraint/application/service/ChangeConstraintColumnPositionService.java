package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.domain.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeConstraintColumnPositionService implements ChangeConstraintColumnPositionUseCase {

  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;

  @Override
  public Mono<MutationResult<Void>> changeConstraintColumnPosition(
      ChangeConstraintColumnPositionCommand command) {
    return Mono.defer(() -> {
      return getConstraintColumnByIdPort.findConstraintColumnById(command.constraintColumnId())
          .switchIfEmpty(Mono.error(new ConstraintColumnNotExistException("Constraint column not found")))
          .flatMap(constraintColumn -> getConstraintByIdPort
              .findConstraintById(constraintColumn.constraintId())
              .switchIfEmpty(Mono.error(new ConstraintNotExistException("Constraint not found")))
              .flatMap(constraint -> getConstraintColumnsByConstraintIdPort
                  .findConstraintColumnsByConstraintId(constraintColumn.constraintId())
                  .defaultIfEmpty(List.of())
                  .flatMap(columns -> reorderColumns(
                      constraintColumn,
                      columns,
                      command.seqNo()))
                  .thenReturn(MutationResult.<Void>of(null, constraint.tableId()))));
    });
  }

  private Mono<Void> reorderColumns(
      ConstraintColumn constraintColumn,
      List<ConstraintColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new ConstraintColumnNotExistException("Constraint column not found"));
    }

    List<ConstraintColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, constraintColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new ConstraintColumnNotExistException("Constraint column not found"));
    }
    ConstraintColumn movingColumn = reordered.remove(currentIndex);
    int normalizedPosition = Math.clamp(nextPosition, 0, columns.size() - 1);
    reordered.add(normalizedPosition, movingColumn);

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
