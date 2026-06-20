package com.schemafy.core.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintColumnPositionCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintColumnPositionUseCase;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintColumnPositionPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = CONSTRAINT_COLUMN, id = "constraintColumnId"))
public class ChangeConstraintColumnPositionService implements ChangeConstraintColumnPositionUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ChangeConstraintColumnPositionPort changeConstraintColumnPositionPort;
  private final GetConstraintColumnByIdPort getConstraintColumnByIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeConstraintColumnPosition(
      ChangeConstraintColumnPositionCommand command) {
    return getConstraintColumnByIdPort
        .findConstraintColumnById(command.constraintColumnId())
        .switchIfEmpty(Mono.error(new DomainException(
            ConstraintErrorCode.COLUMN_NOT_FOUND,
            "Constraint column not found")))
        .flatMap(constraintColumn -> getConstraintByIdPort
            .findConstraintById(constraintColumn.constraintId())
            .switchIfEmpty(Mono.error(new DomainException(
                ConstraintErrorCode.NOT_FOUND,
                "Constraint not found")))
            .flatMap(constraint -> getConstraintColumnsByConstraintIdPort
                .findConstraintColumnsByConstraintId(
                    constraintColumn.constraintId())
                .defaultIfEmpty(List.of())
                .flatMap(columns -> {
                  int currentPosition = resolveCurrentPosition(constraintColumn, columns);
                  int normalizedPosition = Math.clamp(command.seqNo(), 0, columns.size() - 1);
                  if (currentPosition == normalizedPosition) {
                    return Mono.just(MutationResult.<Void>of(null,
                        constraint.tableId()));
                  }
                  List<ConstraintColumn> reordered = reorderColumns(
                      columns,
                      currentPosition,
                      normalizedPosition);
                  return erdMutationCoordinator.coordinate(
                      ErdOperationType.CHANGE_CONSTRAINT_COLUMN_POSITION,
                      command,
                      () -> changeConstraintColumnPositionPort
                          .changeConstraintColumnPositions(
                              constraintColumn.constraintId(), reordered)
                          .thenReturn(MutationResult.<Void>of(null,
                              constraint.tableId())));
                })))
        .as(transactionalOperator::transactional);
  }

  private int resolveCurrentPosition(
      ConstraintColumn constraintColumn,
      List<ConstraintColumn> columns) {
    if (columns.isEmpty()) {
      throw new DomainException(
          ConstraintErrorCode.COLUMN_NOT_FOUND,
          "Constraint column not found");
    }
    int currentPosition = findIndex(columns, constraintColumn.id());
    if (currentPosition < 0) {
      throw new DomainException(
          ConstraintErrorCode.COLUMN_NOT_FOUND,
          "Constraint column not found");
    }
    return currentPosition;
  }

  private List<ConstraintColumn> reorderColumns(
      List<ConstraintColumn> columns,
      int currentIndex,
      int normalizedPosition) {
    List<ConstraintColumn> reordered = new ArrayList<>(columns);
    ConstraintColumn movingColumn = reordered.remove(currentIndex);
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

    return updated;
  }

  private static int findIndex(List<ConstraintColumn> columns,
      String constraintColumnId) {
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
