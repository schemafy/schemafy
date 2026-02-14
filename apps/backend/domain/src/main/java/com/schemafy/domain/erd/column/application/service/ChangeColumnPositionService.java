package com.schemafy.domain.erd.column.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionCommand;
import com.schemafy.domain.erd.column.application.port.in.ChangeColumnPositionUseCase;
import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPositionInvalidException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeColumnPositionService implements ChangeColumnPositionUseCase {

  private final ChangeColumnPositionPort changeColumnPositionPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final TransactionalOperator transactionalOperator;

  @Override
  public Mono<MutationResult<Void>> changeColumnPosition(ChangeColumnPositionCommand command) {
    return Mono.defer(() -> {
      return getColumnByIdPort.findColumnById(command.columnId())
          .switchIfEmpty(Mono.error(new ColumnNotExistException("Column not found")))
          .flatMap(column -> getColumnsByTableIdPort.findColumnsByTableId(column.tableId())
              .defaultIfEmpty(List.of())
              .flatMap(columns -> reorderColumns(column, columns, command.seqNo()))
              .flatMap(reordered -> changeColumnPositionPort
                  .changeColumnPositions(column.tableId(), reordered))
              .thenReturn(MutationResult.<Void>of(null, column.tableId())))
          .as(transactionalOperator::transactional);
    });
  }

  private Mono<List<Column>> reorderColumns(Column targetColumn, List<Column> columns, int nextPosition) {
    if (columns.isEmpty()) {
      return Mono.error(new ColumnPositionInvalidException("Column not found"));
    }

    List<Column> reordered = new ArrayList<>(columns);
    int currentIndex = findIndex(reordered, targetColumn.id());
    if (currentIndex < 0) {
      return Mono.error(new ColumnNotExistException("Column not found"));
    }

    Column movingColumn = reordered.remove(currentIndex);
    int normalizedPosition = Math.clamp(nextPosition, 0, columns.size() - 1);
    reordered.add(normalizedPosition, movingColumn);

    List<Column> updated = new ArrayList<>(reordered.size());
    for (int index = 0; index < reordered.size(); index++) {
      Column column = reordered.get(index);
      updated.add(new Column(
          column.id(),
          column.tableId(),
          column.name(),
          column.dataType(),
          column.lengthScale(),
          index,
          column.autoIncrement(),
          column.charset(),
          column.collation(),
          column.comment()));
    }

    return Mono.just(updated);
  }

  private static int findIndex(List<Column> columns, String columnId) {
    for (int index = 0; index < columns.size(); index++) {
      if (equalsIgnoreCase(columns.get(index).id(), columnId)) {
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
