package com.schemafy.core.erd.index.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionCommand;
import com.schemafy.core.erd.index.application.port.in.ChangeIndexColumnSortDirectionUseCase;
import com.schemafy.core.erd.index.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.core.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.core.erd.index.domain.Index;
import com.schemafy.core.erd.index.domain.IndexColumn;
import com.schemafy.core.erd.index.domain.exception.IndexErrorCode;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.validator.IndexValidator;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.INDEX_COLUMN;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = INDEX_COLUMN, id = "indexColumnId"))
public class ChangeIndexColumnSortDirectionService
    implements ChangeIndexColumnSortDirectionUseCase {

  private final TransactionalOperator transactionalOperator;
  private final ChangeIndexColumnSortDirectionPort changeIndexColumnSortDirectionPort;
  private final GetIndexColumnByIdPort getIndexColumnByIdPort;
  private final GetIndexByIdPort getIndexByIdPort;
  private final GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;
  private final GetIndexesByTableIdPort getIndexesByTableIdPort;
  private final IndexCapabilityResolver indexCapabilityResolver;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

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
            .flatMap(index -> indexCapabilityResolver.resolve(INDEX_COLUMN, indexColumn.id())
                .flatMap(capabilities -> {
                  IndexValidator.validateType(capabilities, index.type());
                  if (indexColumn.sortDirection() == command.sortDirection()) {
                    return Mono.just(MutationResult.<Void>noop(null, index.tableId()));
                  }
                  return erdMutationCoordinator.coordinate(
                      ErdOperationType.CHANGE_INDEX_COLUMN_SORT_DIRECTION,
                      command,
                      () -> getIndexColumnByIdPort.findIndexColumnById(command.indexColumnId())
                          .switchIfEmpty(Mono.error(new DomainException(
                              IndexErrorCode.COLUMN_NOT_FOUND, "Index column not found")))
                          .flatMap(lockedIndexColumn -> getIndexByIdPort.findIndexById(lockedIndexColumn.indexId())
                              .switchIfEmpty(Mono.error(
                                  new DomainException(IndexErrorCode.NOT_FOUND, "Index not found")))
                              .flatMap(lockedIndex -> {
                                if (lockedIndexColumn.sortDirection() == command.sortDirection()) {
                                  return Mono.just(MutationResult.<Void>noop(null, lockedIndex.tableId()));
                                }
                                return validateSortDirectionChange(
                                    lockedIndex,
                                    lockedIndexColumn,
                                    command,
                                    capabilities)
                                    .then(Mono.defer(() -> changeIndexColumnSortDirectionPort
                                        .changeIndexColumnSortDirection(
                                            lockedIndexColumn.id(),
                                            command.sortDirection())))
                                    .thenReturn(MutationResult.<Void>of(null, lockedIndex.tableId()));
                              })));
                })))
        .as(transactionalOperator::transactional);
  }

  private Mono<Void> validateSortDirectionChange(
      Index index,
      IndexColumn indexColumn,
      ChangeIndexColumnSortDirectionCommand command,
      IndexCapabilities capabilities) {
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
                  capabilities,
                  indexes,
                  indexColumnsByIndexId,
                  index.type(),
                  updatedColumns,
                  index.name(),
                  index.id());

              return Mono.empty();
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
