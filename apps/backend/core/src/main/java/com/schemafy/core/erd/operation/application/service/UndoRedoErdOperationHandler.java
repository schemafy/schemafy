package com.schemafy.core.erd.operation.application.service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

interface UndoRedoErdOperationHandler {

  boolean supports(ErdOperationType rootOriginalOpType);

  Mono<MutationResult<Void>> undo(ResolvedUndoRedoEligibility resolved);

  Mono<MutationResult<Void>> redo(ResolvedUndoRedoEligibility resolved);

}
