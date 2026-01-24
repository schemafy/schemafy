package com.schemafy.domain.erd.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.domain.erd.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.application.port.out.DeleteIndexColumnPort;
import com.schemafy.domain.erd.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.domain.IndexColumn;
import com.schemafy.domain.erd.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.domain.exception.IndexNotExistException;

import reactor.core.publisher.Mono;

@Service
public class RemoveIndexColumnService implements RemoveIndexColumnUseCase {

  private final DeleteIndexColumnPort deleteIndexColumnPort;
  private final DeleteIndexPort deleteIndexPort;
  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  public RemoveIndexColumnService(
      DeleteIndexColumnPort deleteIndexColumnPort,
      DeleteIndexPort deleteIndexPort,
      ChangeIndexColumnPositionPort changeIndexColumnPositionPort,
      GetIndexByIdPort getIndexByIdPort,
      GetIndexColumnByIdPort getIndexColumnByIdPort,
      GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort) {
    this.deleteIndexColumnPort = deleteIndexColumnPort;
    this.deleteIndexPort = deleteIndexPort;
    this.changeIndexColumnPositionPort = changeIndexColumnPositionPort;
    this.getIndexByIdPort = getIndexByIdPort;
    this.getIndexColumnByIdPort = getIndexColumnByIdPort;
    this.getIndexColumnsByIndexIdPort = getIndexColumnsByIndexIdPort;
  }

  @Override
  public Mono<Void> removeIndexColumn(RemoveIndexColumnCommand command) {
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new IndexNotExistException("Index not found")))
        .flatMap(index -> getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
            .switchIfEmpty(Mono.error(new IndexColumnNotExistException(
                "Index column not found")))
            .flatMap(indexColumn -> {
              if (!indexColumn.indexId().equalsIgnoreCase(index.id())) {
                return Mono.error(new IndexColumnNotExistException("Index column not found"));
              }
              return deleteIndexColumnPort.deleteIndexColumn(indexColumn.id())
                  .then(handleRemainingColumns(index.id()));
            }));
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
