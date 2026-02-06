package com.schemafy.domain.erd.index.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexColumnCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexCommand;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexResult;
import com.schemafy.domain.erd.index.application.port.in.CreateIndexUseCase;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexNameDuplicateException;
import com.schemafy.domain.erd.index.domain.validator.IndexValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateIndexService implements CreateIndexUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateIndexPort createIndexPort;
  private final CreateIndexColumnPort createIndexColumnPort;
  private final TransactionalOperator transactionalOperator;
  private final IndexExistsPort indexExistsPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Override
  public Mono<MutationResult<CreateIndexResult>> createIndex(CreateIndexCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.name());
      List<CreateIndexColumnCommand> columnCommands = resolveColumnSeqNos(normalizeColumns(command.columns()));

      IndexValidator.validateName(normalizedName);
      IndexValidator.validateType(command.type());

      return getTableByIdPort.findTableById(command.tableId())
          .switchIfEmpty(Mono.error(new TableNotExistException("Table not found")))
          .flatMap(table -> indexExistsPort.existsByTableIdAndName(table.id(), normalizedName)
              .flatMap(exists -> {
                if (exists) {
                  return Mono.error(new IndexNameDuplicateException(
                      "Index name '%s' already exists in table".formatted(normalizedName)));
                }
                return validateAndCreate(table, command, normalizedName, columnCommands)
                    .map(result -> MutationResult.of(result, table.id()));
              }));
    }).as(transactionalOperator::transactional);
  }

  private Mono<CreateIndexResult> validateAndCreate(
      Table table,
      CreateIndexCommand command,
      String normalizedName,
      List<CreateIndexColumnCommand> columnCommands) {
    List<IndexColumn> indexColumns = toColumns(columnCommands);
    List<Integer> seqNos = columnCommands.stream()
        .map(CreateIndexColumnCommand::seqNo)
        .toList();

    return Mono.zip(
        getColumnsByTableIdPort.findColumnsByTableId(table.id()).defaultIfEmpty(List.of()),
        getIndexesByTableIdPort.findIndexesByTableId(table.id()).defaultIfEmpty(List.of()))
        .flatMap(tuple -> fetchIndexColumns(tuple.getT2())
            .flatMap(indexColumnsByIndexId -> {
              List<Column> columns = tuple.getT1();
              List<Index> indexes = tuple.getT2();

              IndexValidator.validateSeqNoIntegrity(seqNos);
              IndexValidator.validateSortDirections(indexColumns, normalizedName);
              IndexValidator.validateColumnExistence(columns, indexColumns, normalizedName);
              IndexValidator.validateColumnUniqueness(indexColumns, normalizedName);
              IndexValidator.validateDefinitionUniqueness(
                  indexes,
                  indexColumnsByIndexId,
                  command.type(),
                  indexColumns,
                  normalizedName,
                  null);

              return persistIndex(table, command, normalizedName, columnCommands);
            }));
  }

  private Mono<CreateIndexResult> persistIndex(
      Table table,
      CreateIndexCommand command,
      String normalizedName,
      List<CreateIndexColumnCommand> columnCommands) {
    return Mono.fromCallable(ulidGeneratorPort::generate)
        .flatMap(indexId -> {
          Index index = new Index(indexId, table.id(), normalizedName, command.type());
          return createIndexPort.createIndex(index)
              .flatMap(savedIndex -> createIndexColumns(indexId, columnCommands)
                  .thenReturn(new CreateIndexResult(
                      savedIndex.id(),
                      savedIndex.name(),
                      savedIndex.type())));
        });
  }

  private Mono<Map<String, List<IndexColumn>>> fetchIndexColumns(List<Index> indexes) {
    if (indexes == null || indexes.isEmpty()) {
      return Mono.just(Map.of());
    }
    return Flux.fromIterable(indexes)
        .flatMap(index -> getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(index.id())
            .defaultIfEmpty(List.of())
            .map(columns -> Map.entry(index.id(), columns)))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  private Mono<Void> createIndexColumns(
      String indexId,
      List<CreateIndexColumnCommand> columnCommands) {
    if (columnCommands.isEmpty()) {
      return Mono.empty();
    }
    return Flux.fromIterable(columnCommands)
        .concatMap(command -> Mono.fromCallable(ulidGeneratorPort::generate)
            .flatMap(id -> createIndexColumnPort.createIndexColumn(
                new IndexColumn(
                    id,
                    indexId,
                    command.columnId(),
                    command.seqNo(),
                    command.sortDirection()))))
        .then();
  }

  private static List<CreateIndexColumnCommand> normalizeColumns(
      List<CreateIndexColumnCommand> columns) {
    if (columns == null) {
      return List.of();
    }
    return List.copyOf(columns);
  }

  private static List<CreateIndexColumnCommand> resolveColumnSeqNos(
      List<CreateIndexColumnCommand> columns) {
    if (columns == null || columns.isEmpty()) {
      return List.of();
    }
    Set<Integer> usedSeqNos = new HashSet<>();
    for (CreateIndexColumnCommand column : columns) {
      if (column.seqNo() != null) {
        usedSeqNos.add(column.seqNo());
      }
    }

    int nextSeqNo = 0;
    List<CreateIndexColumnCommand> resolved = new ArrayList<>(columns.size());
    for (CreateIndexColumnCommand column : columns) {
      Integer seqNo = column.seqNo();
      if (seqNo == null) {
        while (usedSeqNos.contains(nextSeqNo)) {
          nextSeqNo++;
        }
        seqNo = nextSeqNo;
        usedSeqNos.add(seqNo);
        nextSeqNo++;
      }
      resolved.add(new CreateIndexColumnCommand(
          column.columnId(),
          seqNo,
          column.sortDirection()));
    }
    return List.copyOf(resolved);
  }

  private static List<IndexColumn> toColumns(List<CreateIndexColumnCommand> commands) {
    if (commands == null) {
      return List.of();
    }
    return commands.stream()
        .map(command -> new IndexColumn(
            null,
            null,
            command.columnId(),
            command.seqNo(),
            command.sortDirection()))
        .toList();
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

}
