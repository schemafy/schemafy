package com.schemafy.domain.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.RemoveIndexColumnUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.DeleteIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RemoveIndexColumnService implements RemoveIndexColumnUseCase {

  private final DeleteIndexColumnPort deleteIndexColumnPort;
  private final DeleteIndexPort deleteIndexPort;
  private final ChangeIndexColumnPositionPort changeIndexColumnPositionPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

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
