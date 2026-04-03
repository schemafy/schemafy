package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.out.FindErdOperationLogPort;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UndoRedoErdOperationService implements UndoErdOperationUseCase, RedoErdOperationUseCase {

  private final FindErdOperationLogPort findErdOperationLogPort;
  private final List<UndoRedoErdOperationStrategy> strategies;

  @Override
  public Mono<MutationResult<Void>> undo(UndoErdOperationCommand command) {
    Objects.requireNonNull(command, "command");
    return execute(command.opId(), UndoRedoErdOperationStrategy::undo);
  }

  @Override
  public Mono<MutationResult<Void>> redo(RedoErdOperationCommand command) {
    Objects.requireNonNull(command, "command");
    return execute(command.opId(), UndoRedoErdOperationStrategy::redo);
  }

  private Mono<MutationResult<Void>> execute(
      String opId,
      BiFunction<UndoRedoErdOperationStrategy, ErdOperationLog, Mono<MutationResult<Void>>> executor) {
    if (opId == null || opId.isBlank()) {
      return Mono.error(new IllegalArgumentException("opId must not be blank"));
    }

    return findErdOperationLogPort.findByOpId(opId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Operation not found: " + opId)))
        .flatMap(operationLog -> resolveStrategy(operationLog)
            .flatMap(strategy -> executor.apply(strategy, operationLog)));
  }

  private Mono<UndoRedoErdOperationStrategy> resolveStrategy(ErdOperationLog operationLog) {
    return Mono.justOrEmpty(strategies.stream()
        .filter(strategy -> strategy.supports(operationLog.opType()))
        .findFirst())
        .switchIfEmpty(Mono.error(new IllegalArgumentException(
            "Unsupported undo/redo operation: " + operationLog.opType())));
  }

}
