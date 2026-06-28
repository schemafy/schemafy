package com.schemafy.core.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = INDEX_COLUMN, id = "indexColumnId"))
public class ChangeIndexColumnPositionService implements ChangeIndexColumnPositionUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeIndexColumnPosition(
      ChangeIndexColumnPositionCommand command) {
    return getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
        .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.POSITION_INVALID, "Index column not found")))
        .flatMap(indexColumn -> getIndexByIdPort.findIndexById(indexColumn.indexId())
            .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
            .flatMap(index -> getIndexColumnsByIndexIdPort
                .findIndexColumnsByIndexId(indexColumn.indexId())
                .defaultIfEmpty(List.of())
                .flatMap(columns -> {
                  int currentPosition = resolveCurrentPosition(indexColumn, columns);
                  int normalizedPosition = Math.clamp(command.seqNo(), 0, columns.size() - 1);
                  if (currentPosition == normalizedPosition) {
                    return Mono.just(MutationResult.<Void>noop(null, index.tableId()));
                  }
                  return erdMutationCoordinator.coordinate(
                      ErdOperationType.CHANGE_INDEX_COLUMN_POSITION,
                      command,
                      () -> getIndexColumnsByIndexIdPort
                          .findIndexColumnsByIndexId(indexColumn.indexId())
                          .defaultIfEmpty(List.of())
                          .flatMap(lockedColumns -> {
                            int lockedCurrentPosition = resolveCurrentPosition(indexColumn, lockedColumns);
                            int lockedNormalizedPosition = Math.clamp(command.seqNo(), 0, lockedColumns.size() - 1);
                            if (lockedCurrentPosition == lockedNormalizedPosition) {
                              return Mono.just(MutationResult.<Void>noop(null, index.tableId()));
                            }
                            List<IndexColumn> reordered = reorderColumns(
                                lockedColumns,
                                lockedCurrentPosition,
                                lockedNormalizedPosition);
                            return changeIndexColumnPositionPort
                                .changeIndexColumnPositions(indexColumn.indexId(), reordered)
                                .thenReturn(MutationResult.<Void>of(null, index.tableId()));
                          }));
                })))
        .as(transactionalOperator::transactional);
  }

  private int resolveCurrentPosition(
      IndexColumn indexColumn,
      List<IndexColumn> columns) {
    if (columns.isEmpty()) {
      throw new DomainException(IndexErrorCode.POSITION_INVALID, "Index column not found");
    }
    int currentPosition = findIndex(columns, indexColumn.id());
    if (currentPosition < 0) {
      throw new DomainException(IndexErrorCode.POSITION_INVALID, "Index column not found");
    }
    return currentPosition;
  }

  private List<IndexColumn> reorderColumns(
      List<IndexColumn> columns,
      int currentIndex,
      int normalizedPosition) {
    List<IndexColumn> reordered = new ArrayList<>(columns);
    IndexColumn movingColumn = reordered.remove(currentIndex);
    reordered.add(normalizedPosition, movingColumn);

    List<IndexColumn> updated = new ArrayList<>(reordered.size());
    for (int index = 0; index < reordered.size(); index++) {
      IndexColumn column = reordered.get(index);
      updated.add(new IndexColumn(
          column.id(),
          column.indexId(),
          column.columnId(),
          index,
          column.sortDirection()));
    }

    return updated;
  }

  private static int findIndex(List<IndexColumn> columns, String indexColumnId) {
    for (int index = 0; index < columns.size(); index++) {
      if (equalsIgnoreCase(columns.get(index).id(), indexColumnId)) {
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
