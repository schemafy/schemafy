package com.schemafy.domain.erd.constraint.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<AddConstraintColumnResult> addConstraintColumn(AddConstraintColumnCommand command) {
    return Mono.defer(() -> {
      ConstraintValidator.validatePosition(command.seqNo());
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
          .flatMap(constraint -> fetchTableContext(constraint.tableId())
              .flatMap(context -> addColumn(constraint, context, command)));
    });
  }

  private Mono<AddConstraintColumnResult> addColumn(
      Constraint constraint,
      TableContext context,
      AddConstraintColumnCommand command) {
    List<ConstraintColumn> existingColumns = context.constraintColumns()
        .getOrDefault(constraint.id(), List.of());
    List<String> columnIds = new ArrayList<>(existingColumns.size() + 1);
    List<Integer> seqNos = new ArrayList<>(existingColumns.size() + 1);
    for (ConstraintColumn column : existingColumns) {
      columnIds.add(column.columnId());
      seqNos.add(column.seqNo());
    }
    columnIds.add(command.columnId());
    seqNos.add(command.seqNo());

    ConstraintValidator.validateSeqNoIntegrity(seqNos);
    ConstraintValidator.validateColumnExistence(context.columns(), columnIds, constraint.name());
    ConstraintValidator.validateColumnUniqueness(columnIds, constraint.name());
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

    ConstraintColumn constraintColumn = new ConstraintColumn(
        ulidGeneratorPort.generate(),
        constraint.id(),
        command.columnId(),
        command.seqNo());

    return createConstraintColumnPort.createConstraintColumn(constraintColumn)
        .flatMap(savedColumn -> {
          if (constraint.kind() != ConstraintKind.PRIMARY_KEY) {
            return Mono.just(new AddConstraintColumnResult(
                savedColumn.id(),
                savedColumn.constraintId(),
                savedColumn.columnId(),
                savedColumn.seqNo(),
                List.of()));
          }
          return cascadeCreateFkColumns(constraint.tableId(), command.columnId())
              .map(cascadeResults -> new AddConstraintColumnResult(
                  savedColumn.id(),
                  savedColumn.constraintId(),
                  savedColumn.columnId(),
                  savedColumn.seqNo(),
                  cascadeResults));
        });
  }

  private Mono<List<CascadeCreatedColumn>> cascadeCreateFkColumns(
      String pkTableId, String pkColumnId) {
    return getColumnByIdPort.findColumnById(pkColumnId)
        .flatMap(pkColumn -> pkCascadeHelper.cascadeAddPkColumn(
            pkTableId, pkColumn, new HashSet<>())
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

}
