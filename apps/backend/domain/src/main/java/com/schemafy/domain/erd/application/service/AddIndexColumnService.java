package com.schemafy.domain.erd.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.AddIndexColumnCommand;
import com.schemafy.domain.erd.application.port.in.AddIndexColumnResult;
import com.schemafy.domain.erd.application.port.in.AddIndexColumnUseCase;
import com.schemafy.domain.erd.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.Index;
import com.schemafy.domain.erd.domain.IndexColumn;
import com.schemafy.domain.erd.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.domain.validator.IndexValidator;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AddIndexColumnService implements AddIndexColumnUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateIndexColumnPort createIndexColumnPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  public AddIndexColumnService(
      UlidGeneratorPort ulidGeneratorPort,
      CreateIndexColumnPort createIndexColumnPort,
      GetIndexByIdPort getIndexByIdPort,
      GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort,
      GetIndexesByTableIdPort getIndexesByTableIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    this.ulidGeneratorPort = ulidGeneratorPort;
    this.createIndexColumnPort = createIndexColumnPort;
    this.getIndexByIdPort = getIndexByIdPort;
    this.getIndexColumnsByIndexIdPort = getIndexColumnsByIndexIdPort;
    this.getIndexesByTableIdPort = getIndexesByTableIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  public Mono<AddIndexColumnResult> addIndexColumn(AddIndexColumnCommand command) {
    return getIndexByIdPort.findIndexById(command.indexId())
        .switchIfEmpty(Mono.error(new IndexNotExistException("Index not found")))
        .flatMap(index -> validateAndAdd(index, command));
  }

  private Mono<AddIndexColumnResult> validateAndAdd(Index index, AddIndexColumnCommand command) {
    return Mono.zip(
        getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(index.id())
            .defaultIfEmpty(List.of()),
        getColumnsByTableIdPort.findColumnsByTableId(index.tableId())
            .defaultIfEmpty(List.of()),
        getIndexesByTableIdPort.findIndexesByTableId(index.tableId())
            .defaultIfEmpty(List.of()))
        .flatMap(tuple -> fetchIndexColumns(tuple.getT3())
            .flatMap(indexColumnsByIndexId -> {
              List<IndexColumn> existingColumns = tuple.getT1();
              List<Column> tableColumns = tuple.getT2();
              List<Index> indexes = tuple.getT3();

              List<IndexColumn> updatedColumns = new ArrayList<>(existingColumns.size() + 1);
              updatedColumns.addAll(existingColumns);
              updatedColumns.add(new IndexColumn(
                  null,
                  index.id(),
                  command.columnId(),
                  command.seqNo(),
                  command.sortDirection()));

              List<Integer> seqNos = updatedColumns.stream()
                  .map(IndexColumn::seqNo)
                  .toList();

              IndexValidator.validateSeqNoIntegrity(seqNos);
              IndexValidator.validateSortDirections(updatedColumns, index.name());
              IndexValidator.validateColumnExistence(tableColumns, updatedColumns, index.name());
              IndexValidator.validateColumnUniqueness(updatedColumns, index.name());
              IndexValidator.validateDefinitionUniqueness(
                  indexes,
                  indexColumnsByIndexId,
                  index.type(),
                  updatedColumns,
                  index.name(),
                  index.id());

              IndexColumn indexColumn = new IndexColumn(
                  ulidGeneratorPort.generate(),
                  index.id(),
                  command.columnId(),
                  command.seqNo(),
                  command.sortDirection());

              return createIndexColumnPort.createIndexColumn(indexColumn)
                  .map(savedColumn -> new AddIndexColumnResult(
                      savedColumn.id(),
                      savedColumn.indexId(),
                      savedColumn.columnId(),
                      savedColumn.seqNo(),
                      savedColumn.sortDirection()));
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
