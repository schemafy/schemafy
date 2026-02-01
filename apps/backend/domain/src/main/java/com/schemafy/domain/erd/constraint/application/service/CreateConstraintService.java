package com.schemafy.domain.erd.constraint.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintColumnCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintCommand;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintResult;
import com.schemafy.domain.erd.constraint.application.port.in.CreateConstraintUseCase;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.domain.validator.ConstraintValidator;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateConstraintService implements CreateConstraintUseCase {

  private final UlidGeneratorPort ulidGeneratorPort;
  private final CreateConstraintPort createConstraintPort;
  private final CreateConstraintColumnPort createConstraintColumnPort;
  private final ConstraintExistsPort constraintExistsPort;
  private final GetTableByIdPort getTableByIdPort;
  private final GetColumnsByTableIdPort getColumnsByTableIdPort;
  private final GetColumnByIdPort getColumnByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private final PkCascadeHelper pkCascadeHelper;

  @Override
  public Mono<CreateConstraintResult> createConstraint(CreateConstraintCommand command) {
    return Mono.defer(() -> {
      String normalizedName = normalizeName(command.name());
      String normalizedCheckExpr = normalizeOptional(command.checkExpr());
      String normalizedDefaultExpr = normalizeOptional(command.defaultExpr());
      List<CreateConstraintColumnCommand> columnCommands = normalizeColumns(command.columns());

      ConstraintValidator.validateName(normalizedName);
      columnCommands.forEach(column -> ConstraintValidator.validatePosition(column.seqNo()));

      return getTableByIdPort.findTableById(command.tableId())
          .switchIfEmpty(Mono.error(new TableNotExistException("Table not found")))
          .flatMap(table -> constraintExistsPort.existsBySchemaIdAndName(table.schemaId(), normalizedName)
              .flatMap(exists -> {
                if (exists) {
                  return Mono.error(new ConstraintNameDuplicateException(
                      "Constraint name '%s' already exists in schema".formatted(normalizedName)));
                }
                return fetchTableContext(table.id())
                    .flatMap(context -> createConstraint(
                        table,
                        context,
                        command.kind(),
                        normalizedName,
                        normalizedCheckExpr,
                        normalizedDefaultExpr,
                        columnCommands));
              }));
    });
  }

  private Mono<CreateConstraintResult> createConstraint(
      Table table,
      TableContext context,
      ConstraintKind kind,
      String name,
      String checkExpr,
      String defaultExpr,
      List<CreateConstraintColumnCommand> columnCommands) {
    if (kind == null) {
      return Mono.error(new InvalidValueException("Constraint kind must not be null"));
    }

    List<String> columnIds = columnCommands.stream()
        .map(CreateConstraintColumnCommand::columnId)
        .toList();
    List<Integer> seqNos = columnCommands.stream()
        .map(CreateConstraintColumnCommand::seqNo)
        .toList();

    ConstraintValidator.validateSeqNoIntegrity(seqNos);
    ConstraintValidator.validateColumnExistence(context.columns(), columnIds, name);
    ConstraintValidator.validateColumnUniqueness(columnIds, name);
    ConstraintValidator.validatePrimaryKeySingle(context.constraints(), kind, null);
    ConstraintValidator.validateDefinitionUniqueness(
        context.constraints(),
        context.constraintColumns(),
        kind,
        checkExpr,
        defaultExpr,
        columnIds,
        name,
        null);
    ConstraintValidator.validateUniqueSameAsPrimaryKey(
        context.constraints(),
        context.constraintColumns(),
        kind,
        columnIds,
        name,
        null);

    Constraint constraint = new Constraint(
        ulidGeneratorPort.generate(),
        table.id(),
        name,
        kind,
        checkExpr,
        defaultExpr);

    return createConstraintPort.createConstraint(constraint)
        .flatMap(savedConstraint -> createConstraintColumns(savedConstraint.id(), columnCommands)
            .then(cascadeCreateFkColumnsIfPk(kind, table.id(), columnIds))
            .thenReturn(new CreateConstraintResult(
                savedConstraint.id(),
                savedConstraint.name(),
                savedConstraint.kind(),
                savedConstraint.checkExpr(),
                savedConstraint.defaultExpr())));
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

  private Mono<Void> createConstraintColumns(
      String constraintId,
      List<CreateConstraintColumnCommand> columns) {
    if (columns.isEmpty()) {
      return Mono.empty();
    }
    return Flux.fromIterable(columns)
        .concatMap(column -> createConstraintColumnPort.createConstraintColumn(
            new ConstraintColumn(
                ulidGeneratorPort.generate(),
                constraintId,
                column.columnId(),
                column.seqNo())))
        .then();
  }

  private static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static List<CreateConstraintColumnCommand> normalizeColumns(
      List<CreateConstraintColumnCommand> columns) {
    if (columns == null) {
      return List.of();
    }
    return List.copyOf(columns);
  }

  private Mono<Void> cascadeCreateFkColumnsIfPk(
      ConstraintKind kind, String pkTableId, List<String> pkColumnIds) {
    if (kind != ConstraintKind.PRIMARY_KEY) {
      return Mono.empty();
    }
    return Flux.fromIterable(pkColumnIds)
        .concatMap(pkColumnId -> getColumnByIdPort.findColumnById(pkColumnId)
            .flatMap(pkColumn -> pkCascadeHelper.cascadeAddPkColumn(
                pkTableId, pkColumn, new HashSet<>())))
        .then();
  }

  private record TableContext(
      List<Column> columns,
      List<Constraint> constraints,
      Map<String, List<ConstraintColumn>> constraintColumns) {
  }

}
