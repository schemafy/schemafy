package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnResult.CascadeCreatedColumn;
import com.schemafy.domain.erd.constraint.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.domain.validator.ConstraintValidator;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AddConstraintColumnService implements AddConstraintColumnUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateConstraintColumnPort createConstraintColumnPort;
  private final TransactionalOperator transactionalOperator;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<MutationResult<AddConstraintColumnResult>> addConstraintColumn(
      AddConstraintColumnCommand command) {
    return Mono.defer(() -> {
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new ConstraintNotExistException("Constraint not found")))
          .flatMap(constraint -> {
            Set<String> affectedTableIds = new HashSet<>();
            affectedTableIds.add(constraint.tableId());
            return fetchTableContext(constraint.tableId())
                .flatMap(context -> addColumn(
                    constraint,
                    context,
                    command,
                    affectedTableIds));
          });
    }).as(transactionalOperator::transactional);
  }

  private Mono<MutationResult<AddConstraintColumnResult>> addColumn(
      Constraint constraint,
      TableContext context,
      AddConstraintColumnCommand command,
      Set<String> affectedTableIds) {
    List<ConstraintColumn> existingColumns = context.constraintColumns()
        .getOrDefault(constraint.id(), List.of());
    int resolvedSeqNo = resolveSeqNo(command.seqNo(), existingColumns);
    ConstraintValidator.validatePosition(resolvedSeqNo);
    List<String> columnIds = new ArrayList<>(existingColumns.size() + 1);
    List<Integer> seqNos = new ArrayList<>(existingColumns.size() + 1);
    for (ConstraintColumn column : existingColumns) {
      columnIds.add(column.columnId());
      seqNos.add(column.seqNo());
    }
    columnIds.add(command.columnId());
    seqNos.add(resolvedSeqNo);

    ConstraintValidator.validateSeqNoIntegrity(seqNos);
    ConstraintValidator.validateColumnExistence(context.columns(), columnIds, constraint.name());
    ConstraintValidator.validateColumnUniqueness(columnIds, constraint.name());
    ConstraintValidator.validateDefaultColumnCardinality(constraint.kind(), columnIds);
    ConstraintValidator.validatePrimaryKeySingle(
        context.constraints(),
        constraint.kind(),
        constraint.id());
    ConstraintValidator.validateDefinitionUniqueness(
        context.constraints(),
        context.constraintColumns(),
        constraint.kind(),
        constraint.checkExpr(),
        constraint.defaultExpr(),
        columnIds,
        constraint.name(),
        constraint.id());
    ConstraintValidator.validateUniqueSameAsPrimaryKey(
        context.constraints(),
        context.constraintColumns(),
        constraint.kind(),
        columnIds,
        constraint.name(),
        constraint.id());

    return Mono.fromCallable(ulidGeneratorPort::generate)
        .flatMap(id -> {
          ConstraintColumn constraintColumn = new ConstraintColumn(
              id,
              constraint.id(),
              command.columnId(),
              resolvedSeqNo);

          return createConstraintColumnPort.createConstraintColumn(constraintColumn)
              .flatMap(savedColumn -> {
                if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
                  AddConstraintColumnResult result = new AddConstraintColumnResult(
                      savedColumn.id(),
                      savedColumn.constraintId(),
                      savedColumn.columnId(),
                      savedColumn.seqNo(),
                      List.of());
                  return Mono.just(MutationResult.of(result, affectedTableIds));
                }
                return cascadeCreateFkColumns(constraint.tableId(), command.columnId(), affectedTableIds)
                    .map(cascadeResults -> {
                      cascadeResults.forEach(info -> affectedTableIds.add(info.fkTableId()));
                      AddConstraintColumnResult result = new AddConstraintColumnResult(
                          savedColumn.id(),
                          savedColumn.constraintId(),
                          savedColumn.columnId(),
                          savedColumn.seqNo(),
                          cascadeResults);
                      return MutationResult.of(result, affectedTableIds);
                    });
              });
        });
  }

  private Mono<List<CascadeCreatedColumn>> cascadeCreateFkColumns(
      String pkTableId,
      String pkColumnId,
      Set<String> affectedTableIds) {
    return getColumnByIdPort.findColumnById(pkColumnId)
        .flatMap(pkColumn -> pkCascadeHelper.cascadeAddPkColumn(
            pkTableId,
            pkColumn,
            new HashSet<>(),
            affectedTableIds)
            .map(cascadeInfoList -> cascadeInfoList.stream()
                .map(info -> new CascadeCreatedColumn(
                    info.fkColumnId(),
                    info.fkColumnName(),
                    info.fkTableId(),
                    info.relationshipColumnId(),
                    info.relationshipId(),
                    info.constraintColumnId(),
                    info.constraintId()))
                .toList()))
        .defaultIfEmpty(List.of());
  }

  private Mono<TableContext> fetchTableContext(String tableId) {
    Mono<List<Column>> columnsMono = getColumnsByTableIdPort.findColumnsByTableId(tableId)
        .defaultIfEmpty(List.of());
    Mono<List<Constraint>> constraintsMono = getConstraintsByTableIdPort.findConstraintsByTableId(tableId)
        .defaultIfEmpty(List.of());

    return Mono.zip(columnsMono, constraintsMono)
        .flatMap(tuple -> fetchConstraintColumns(tuple.getT2())
            .map(columnsByConstraintId -> new TableContext(
                tuple.getT1(),
                tuple.getT2(),
                columnsByConstraintId)));
  }

  private Mono<Map<String, List<ConstraintColumn>>> fetchConstraintColumns(List<Constraint> constraints) {
    if (constraints == null || constraints.isEmpty()) {
      return Mono.just(Map.of());
    }
    return Flux.fromIterable(constraints)
        .flatMap(constraint -> getConstraintColumnsByConstraintIdPort
            .findConstraintColumnsByConstraintId(constraint.id())
            .defaultIfEmpty(List.of())
            .map(columns -> Map.entry(constraint.id(), columns)))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  private record TableContext(
      List<Column> columns,
      List<Constraint> constraints,
      Map<String, List<ConstraintColumn>> constraintColumns) {
  }

  private static int resolveSeqNo(Integer requestedSeqNo, List<ConstraintColumn> existingColumns) {
    if (requestedSeqNo != null) {
      return requestedSeqNo;
    }
    return existingColumns.stream()
        .mapToInt(ConstraintColumn::seqNo)
        .max()
        .orElse(-1) + 1;
  }

}
