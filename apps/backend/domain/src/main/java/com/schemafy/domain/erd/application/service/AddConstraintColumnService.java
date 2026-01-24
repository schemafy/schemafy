package com.schemafy.domain.erd.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.schemafy.domain.erd.application.port.in.AddConstraintColumnCommand;
import com.schemafy.domain.erd.application.port.in.AddConstraintColumnResult;
import com.schemafy.domain.erd.application.port.in.AddConstraintColumnUseCase;
import com.schemafy.domain.erd.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.domain.Column;
import com.schemafy.domain.erd.domain.Constraint;
import com.schemafy.domain.erd.domain.ConstraintColumn;
import com.schemafy.domain.erd.domain.validator.ConstraintValidator;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AddConstraintColumnService implements AddConstraintColumnUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateConstraintColumnPort createConstraintColumnPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;

  public AddConstraintColumnService(
      UlidGeneratorPort ulidGeneratorPort,
      CreateConstraintColumnPort createConstraintColumnPort,
      GetConstraintByIdPort getConstraintByIdPort,
      GetConstraintsByTableIdPort getConstraintsByTableIdPort,
      GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort,
      GetColumnsByTableIdPort getColumnsByTableIdPort) {
    this.ulidGeneratorPort = ulidGeneratorPort;
    this.createConstraintColumnPort = createConstraintColumnPort;
    this.getConstraintByIdPort = getConstraintByIdPort;
    this.getConstraintsByTableIdPort = getConstraintsByTableIdPort;
    this.getConstraintColumnsByConstraintIdPort = getConstraintColumnsByConstraintIdPort;
    this.getColumnsByTableIdPort = getColumnsByTableIdPort;
  }

  @Override
  public Mono<AddConstraintColumnResult> addConstraintColumn(AddConstraintColumnCommand command) {
    ConstraintValidator.validatePosition(command.seqNo());
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new RuntimeException("Constraint not found")))
        .flatMap(constraint -> fetchTableContext(constraint.tableId())
            .flatMap(context -> addColumn(constraint, context, command)));
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
        .map(savedColumn -> new AddConstraintColumnResult(
            savedColumn.id(),
            savedColumn.constraintId(),
            savedColumn.columnId(),
            savedColumn.seqNo()));
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
