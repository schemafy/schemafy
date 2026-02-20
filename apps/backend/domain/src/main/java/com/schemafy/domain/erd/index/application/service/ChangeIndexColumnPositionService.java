package com.schemafy.domain.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeIndexColumnPositionService implements ChangeIndexColumnPositionUseCase {

  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetIndexByIdPort getIndexByIdPort;

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
                .flatMap(columns -> reorderColumns(indexColumn, columns, command.seqNo()))
                .thenReturn(MutationResult.<Void>of(null, index.tableId()))));
  }

  private Mono<Void> reorderColumns(
      IndexColumn indexColumn,
      List<IndexColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new DomainException(IndexErrorCode.POSITION_INVALID, "Index column not found"));
    }

    List<IndexColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, indexColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new DomainException(IndexErrorCode.POSITION_INVALID, "Index column not found"));
    }
    IndexColumn movingColumn = reordered.remove(currentIndex);
    int normalizedPosition = Math.clamp(nextPosition, 0, columns.size() - 1);
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

    return changeIndexColumnPositionPort.changeIndexColumnPositions(indexColumn.indexId(), updated);
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
