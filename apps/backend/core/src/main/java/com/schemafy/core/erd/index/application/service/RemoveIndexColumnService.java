package com.schemafy.core.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.core.erd.index.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexColumnPort;
import com.schemafy.core.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RemoveIndexColumnService implements RemoveIndexColumnUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteIndexColumnPort deleteIndexColumnPort;
  private final DeleteIndexPort deleteIndexPort;
  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> removeIndexColumn(RemoveIndexColumnCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.REMOVE_INDEX_COLUMN, command,
        () -> getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found")))
        .flatMap(indexColumn -> getIndexByIdPort.findIndexById(indexColumn.indexId())
            .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
            .flatMap(index -> deleteIndexColumnPort.deleteIndexColumn(indexColumn.id())
                .then(handleRemainingColumns(index.id()))
                .thenReturn(MutationResult.<Void>of(null, index.tableId())))))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> handleRemainingColumns(String indexId) {
    return getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(indexId)
        .defaultIfEmpty(List.of())
        .flatMap(columns -> {
          if (columns.isEmpty()) {
            return deleteIndexPort.deleteIndex(indexId);
          }
          List<IndexColumn> reordered = new ArrayList<>(columns.size());
          for (int index = 0; index < columns.size(); index++) {
            IndexColumn column = columns.get(index);
            reordered.add(new IndexColumn(
                column.id(),
                column.indexId(),
                column.columnId(),
                index,
                column.sortDirection()));
          }
          return changeIndexColumnPositionPort.changeIndexColumnPositions(indexId, reordered);
        });
  }

}
