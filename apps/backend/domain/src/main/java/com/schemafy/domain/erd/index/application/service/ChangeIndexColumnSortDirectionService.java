package com.schemafy.domain.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.domain.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.domain.erd.index.domain.validator.IndexValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeIndexColumnSortDirectionService
    implements ChangeIndexColumnSortDirectionUseCase {

  private final ChangeIndexColumnSortDirectionPort changeIndexColumnSortDirectionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Override
  public Mono<MutationResult<Void>> changeIndexColumnSortDirection(
      ChangeIndexColumnSortDirectionCommand command) {
    if (command.sortDirection() == null) {
      return Mono.error(new DomainException(
          IndexErrorCode.COLUMN_SORT_DIRECTION_INVALID,
          "Sort direction is invalid for index column"));
    }
    return getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
        .switchIfEmpty(Mono.error(new DomainException(
            IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found")))
        .flatMap(indexColumn -> getIndexByIdPort.findIndexById(indexColumn.indexId())
            .switchIfEmpty(Mono.error(new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
            .flatMap(index -> validateAndChange(index, indexColumn, command)
                .thenReturn(MutationResult.<Void>of(null, index.tableId()))));
  }

  private Mono<Void> validateAndChange(
      Index index,
      IndexColumn indexColumn,
      ChangeIndexColumnSortDirectionCommand command) {
    return Mono.zip(
        getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(index.id())
            .defaultIfEmpty(List.of()),
        getIndexesByTableIdPort.findIndexesByTableId(index.tableId())
            .defaultIfEmpty(List.of()))
        .flatMap(tuple -> fetchIndexColumns(tuple.getT2())
            .flatMap(indexColumnsByIndexId -> {
              List<IndexColumn> columns = tuple.getT1();
              List<Index> indexes = tuple.getT2();

              List<IndexColumn> updatedColumns = new ArrayList<>(columns.size());
              for (IndexColumn column : columns) {
                if (column.id().equalsIgnoreCase(indexColumn.id())) {
                  updatedColumns.add(new IndexColumn(
                      column.id(),
                      column.indexId(),
                      column.columnId(),
                      column.seqNo(),
                      command.sortDirection()));
                } else {
                  updatedColumns.add(column);
                }
              }

              IndexValidator.validateSortDirections(updatedColumns, index.name());
              IndexValidator.validateDefinitionUniqueness(
                  indexes,
                  indexColumnsByIndexId,
                  index.type(),
                  updatedColumns,
                  index.name(),
                  index.id());

              return changeIndexColumnSortDirectionPort
                  .changeIndexColumnSortDirection(indexColumn.id(), command.sortDirection());
            }));
  }

  private Mono<Map<String, List<IndexColumn>>> fetchIndexColumns(List<Index> indexes) {
    if (indexes == null || indexes.isEmpty()) {
      return Mono.just(Map.of());
    }
    return Flux.fromIterable(indexes)
        .flatMap(indexItem -> getIndexColumnsByIndexIdPort
            .findIndexColumnsByIndexId(indexItem.id())
            .defaultIfEmpty(List.of())
            .map(columns -> Map.entry(indexItem.id(), columns)))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

}
