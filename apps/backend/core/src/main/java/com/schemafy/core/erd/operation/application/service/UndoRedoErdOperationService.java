package com.schemafy.core.erd.operation.application.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UndoRedoErdOperationService implements UndoErdOperationUseCase,
    RedoErdOperationUseCase {

  private final UndoRedoEligibilityService undoRedoEligibilityService;
  private final List<UndoRedoErdOperationHandler> handlers;

  @Override
  public Mono<MutationResult<Void>> undo(UndoErdOperationCommand command) {
    Objects.requireNonNull(command, "command");
    return undoRedoEligibilityService.resolve(UndoRedoAction.UNDO, command.opId())
        .flatMap(resolved -> execute(resolved, handler -> handler.undo(resolved)));
  }

  @Override
  public Mono<MutationResult<Void>> redo(RedoErdOperationCommand command) {
    Objects.requireNonNull(command, "command");
    return undoRedoEligibilityService.resolve(UndoRedoAction.REDO, command.opId())
        .flatMap(resolved -> execute(resolved, handler -> handler.redo(resolved)));
  }

  private Mono<MutationResult<Void>> execute(
      ResolvedUndoRedoEligibility resolved,
      Function<UndoRedoErdOperationHandler, Mono<MutationResult<Void>>> executor) {
    ErdOperationType rootOriginalOpType = resolved.targetRootOriginalOperation()
        .opType();
    return Mono.justOrEmpty(handlers.stream()
        .filter(handler -> handler.supports(rootOriginalOpType))
        .findFirst())
        .switchIfEmpty(Mono.error(new DomainException(
            OperationErrorCode.UNSUPPORTED,
            "Undo/redo is not supported for operation type: " + rootOriginalOpType)))
        .flatMap(executor);
  }

}
