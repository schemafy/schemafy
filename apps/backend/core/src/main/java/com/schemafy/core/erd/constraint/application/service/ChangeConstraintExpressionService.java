package com.schemafy.core.erd.constraint.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
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
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintCheckExprInverse;
import com.schemafy.core.erd.operation.application.inverse.ChangeConstraintDefaultExprInverse;
import com.schemafy.core.erd.operation.application.service.ErdMutationCoordinator;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.project.application.access.AccessTarget;
import com.schemafy.core.project.application.access.RequireProjectAccess;
import com.schemafy.core.project.domain.ProjectRole;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.schemafy.core.project.application.access.ProjectAccessResourceType.CONSTRAINT;

@Service
@RequiredArgsConstructor
@RequireProjectAccess(role = ProjectRole.EDITOR, target = @AccessTarget(value = CONSTRAINT, id = "constraintId"))
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
    String normalizedCheckExpr = normalizeOptional(command.checkExpr());
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
        .flatMap(constraint -> {
          validateKind(constraint.kind(), ConstraintKind.CHECK, "check expression");
          return changeCheckExpression(
              ErdOperationType.CHANGE_CONSTRAINT_CHECK_EXPR,
              command,
              constraint,
              normalizedCheckExpr);
        });
  }

  @Override
  public Mono<MutationResult<Void>> changeConstraintDefaultExpr(
      ChangeConstraintDefaultExprCommand command) {
    String normalizedDefaultExpr = normalizeOptional(command.defaultExpr());
    return getConstraintByIdPort.findConstraintById(command.constraintId())
        .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
        .flatMap(constraint -> {
          validateKind(constraint.kind(), ConstraintKind.DEFAULT, "default expression");
          return changeDefaultExpression(
              ErdOperationType.CHANGE_CONSTRAINT_DEFAULT_EXPR,
              command,
              constraint,
              normalizedDefaultExpr);
        });
  }

  private Mono<MutationResult<Void>> changeCheckExpression(
      ErdOperationType operationType,
      Object payload,
      Constraint constraint,
      String checkExpr) {
    if (Objects.equals(constraint.checkExpr(), checkExpr)) {
      return Mono.just(MutationResult.<Void>noop(null, constraint.tableId()));
    }
    return erdMutationCoordinator.coordinate(operationType, payload,
        () -> getConstraintByIdPort.findConstraintById(constraint.id())
            .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
            .flatMap(lockedConstraint -> {
              validateKind(lockedConstraint.kind(), ConstraintKind.CHECK, "check expression");
              if (Objects.equals(lockedConstraint.checkExpr(), checkExpr)) {
                return Mono.just(MutationResult.<Void>noop(null, lockedConstraint.tableId()));
              }
              return validateExpressionChange(
                  lockedConstraint,
                  checkExpr,
                  lockedConstraint.defaultExpr())
                  .then(Mono.defer(() -> changeConstraintExpressionPort
                      .changeConstraintExpressions(
                          lockedConstraint.id(),
                          checkExpr,
                          lockedConstraint.defaultExpr())))
                  .thenReturn(MutationResult.<Void>of(null, lockedConstraint.tableId())
                      .withInverse(new ChangeConstraintCheckExprInverse(
                          lockedConstraint.id(),
                          lockedConstraint.checkExpr())));
            }));
  }

  private Mono<MutationResult<Void>> changeDefaultExpression(
      ErdOperationType operationType,
      Object payload,
      Constraint constraint,
      String defaultExpr) {
    if (Objects.equals(constraint.defaultExpr(), defaultExpr)) {
      return Mono.just(MutationResult.<Void>noop(null, constraint.tableId()));
    }
    return erdMutationCoordinator.coordinate(operationType, payload,
        () -> getConstraintByIdPort.findConstraintById(constraint.id())
            .switchIfEmpty(Mono.error(new DomainException(ConstraintErrorCode.NOT_FOUND, "Constraint not found")))
            .flatMap(lockedConstraint -> {
              validateKind(lockedConstraint.kind(), ConstraintKind.DEFAULT, "default expression");
              if (Objects.equals(lockedConstraint.defaultExpr(), defaultExpr)) {
                return Mono.just(MutationResult.<Void>noop(null, lockedConstraint.tableId()));
              }
              return validateExpressionChange(
                  lockedConstraint,
                  lockedConstraint.checkExpr(),
                  defaultExpr)
                  .then(Mono.defer(() -> changeConstraintExpressionPort
                      .changeConstraintExpressions(
                          lockedConstraint.id(),
                          lockedConstraint.checkExpr(),
                          defaultExpr)))
                  .thenReturn(MutationResult.<Void>of(null, lockedConstraint.tableId())
                      .withInverse(new ChangeConstraintDefaultExprInverse(
                          lockedConstraint.id(),
                          lockedConstraint.defaultExpr())));
            }));
  }

  private Mono<Void> validateExpressionChange(
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
              return Mono.empty();
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
