package com.schemafy.core.erd.constraint.application.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintCheckExprUseCase;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprCommand;
import com.schemafy.core.erd.constraint.application.port.in.ChangeConstraintDefaultExprUseCase;
import com.schemafy.core.erd.constraint.application.port.out.ChangeConstraintExpressionPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.ConstraintColumn;
import com.schemafy.core.erd.constraint.domain.exception.ConstraintErrorCode;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.constraint.domain.validator.ConstraintValidator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChangeConstraintExpressionService implements
    ChangeConstraintCheckExprUseCase,
    ChangeConstraintDefaultExprUseCase {

  private final ChangeConstraintExpressionPort changeConstraintExpressionPort;
  private final GetConstraintByIdPort getConstraintByIdPort;
  private final GetConstraintsByTableIdPort getConstraintsByTableIdPort;
  private final GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;
  private ErdMutationCoordinator erdMutationCoordinator = ErdMutationCoordinator.noop();

  @Autowired
  void setErdMutationCoordinator(ErdMutationCoordinator erdMutationCoordinator) {
    this.erdMutationCoordinator = erdMutationCoordinator;
  }

  @Override
  public Mono<MutationResult<Void>> changeConstraintCheckExpr(
      ChangeConstraintCheckExprCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_CONSTRAINT_CHECK_EXPR, command,
        () -> Mono.defer(() -> {
      String normalizedCheckExpr = normalizeOptional(command.checkExpr());
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
          .flatMap(constraint -> {
            validateKind(constraint.kind(), ConstraintKind.CHECK, "check expression");
            return changeExpression(constraint, normalizedCheckExpr, constraint.defaultExpr());
          });
        }));
  }

  @Override
  public Mono<MutationResult<Void>> changeConstraintDefaultExpr(
      ChangeConstraintDefaultExprCommand command) {
    return erdMutationCoordinator.coordinate(ErdOperationType.CHANGE_CONSTRAINT_DEFAULT_EXPR, command,
        () -> Mono.defer(() -> {
      String normalizedDefaultExpr = normalizeOptional(command.defaultExpr());
      return getConstraintByIdPort.findConstraintById(command.constraintId())
          .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
          .flatMap(constraint -> {
            validateKind(constraint.kind(), ConstraintKind.DEFAULT, "default expression");
            return changeExpression(constraint, constraint.checkExpr(), normalizedDefaultExpr);
          });
        }));
  }

  private Mono<MutationResult<Void>> changeExpression(
      Constraint constraint,
      String checkExpr,
      String defaultExpr) {
    return getConstraintsByTableIdPort.findConstraintsByTableId(constraint.tableId())
        .defaultIfEmpty(List.of())
        .flatMap(constraints -> fetchConstraintColumns(constraints)
            .flatMap(constraintColumns -> {
              List<String> columnIds = toColumnIds(
                  constraintColumns.getOrDefault(constraint.id(), List.of()));
              ConstraintValidator.validateDefinitionUniqueness(
                  constraints,
                  constraintColumns,
                  constraint.kind(),
                  checkExpr,
                  defaultExpr,
                  columnIds,
                  constraint.name(),
                  constraint.id());
              return changeConstraintExpressionPort
                  .changeConstraintExpressions(constraint.id(), checkExpr, defaultExpr)
                  .thenReturn(MutationResult.<Void>of(null, constraint.tableId()));
            }));
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

  private static List<String> toColumnIds(List<ConstraintColumn> columns) {
    if (columns == null || columns.isEmpty()) {
      return List.of();
    }
    return columns.stream()
        .map(ConstraintColumn::columnId)
        .toList();
  }

  private static void validateKind(ConstraintKind actual, ConstraintKind expected, String fieldName) {
    if (actual != expected) {
      throw new DomainException(ConstraintErrorCode.INVALID_VALUE,
          "Only %s constraints can change %s".formatted(expected.name(), fieldName));
    }
  }

  private static String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

}
