package com.schemafy.domain.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnPositionUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexPositionInvalidException;

import reactor.core.publisher.Mono;

@Service
public class ChangeIndexColumnPositionService implements ChangeIndexColumnPositionUseCase {

  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  public ChangeIndexColumnPositionService(
      ChangeIndexColumnPositionPort changeIndexColumnPositionPort,
      GetIndexColumnByIdPort getIndexColumnByIdPort,
      GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort) {
    this.changeIndexColumnPositionPort = changeIndexColumnPositionPort;
    this.getIndexColumnByIdPort = getIndexColumnByIdPort;
    this.getIndexColumnsByIndexIdPort = getIndexColumnsByIndexIdPort;
  }

  @Override
  public Mono<Void> changeIndexColumnPosition(ChangeIndexColumnPositionCommand command) {
    if (command.seqNo() < 0) {
      return Mono.error(new IndexPositionInvalidException(
          "Index column position must be zero or positive"));
    }
    return getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
        .switchIfEmpty(Mono.error(new IndexPositionInvalidException("Index column not found")))
        .flatMap(indexColumn -> getIndexColumnsByIndexIdPort
            .findIndexColumnsByIndexId(indexColumn.indexId())
            .defaultIfEmpty(List.of())
            .flatMap(columns -> reorderColumns(indexColumn, columns, command.seqNo())));
  }

  private Mono<Void> reorderColumns(
      IndexColumn indexColumn,
      List<IndexColumn> columns,
      int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new IndexPositionInvalidException("Index column not found"));
    }
    if (nextPosition < 0 || nextPosition >= columns.size()) {
      return Mono.error(new IndexPositionInvalidException(
          "Index column position must be between 0 and %d".formatted(columns.size() - 1)));
    }

    List<IndexColumn> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, indexColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new IndexPositionInvalidException("Index column not found"));
    }
    IndexColumn movingColumn = reordered.remove(currentIndex);
    reordered.add(nextPosition, movingColumn);

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
