package com.schemafy.core.erd.operation.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class SameCommandUndoRedoHandler implements UndoRedoErdOperationHandler {

  private final SameCommandUndoRedoStrategy strategy;

  @Override
  public boolean supports(ErdOperationType rootOriginalOpType) {
    return strategy.supports(rootOriginalOpType);
  }

  @Override
  public Mono<MutationResult<Void>> undo(ResolvedUndoRedoEligibility resolved) {
    Objects.requireNonNull(resolved, "resolved");
    return strategy.undo(resolved.executionBaseOperation());
  }

  @Override
  public Mono<MutationResult<Void>> redo(ResolvedUndoRedoEligibility resolved) {
    Objects.requireNonNull(resolved, "resolved");
    return strategy.redo(resolved.executionBaseOperation());
  }

}
