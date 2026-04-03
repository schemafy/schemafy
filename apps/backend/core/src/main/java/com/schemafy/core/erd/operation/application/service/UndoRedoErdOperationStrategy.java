package com.schemafy.core.erd.operation.application.service;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;

import reactor.core.publisher.Mono;

interface UndoRedoErdOperationStrategy {

  boolean supports(ErdOperationType opType);

  Mono<MutationResult<Void>> undo(ErdOperationLog operationLog);

  Mono<MutationResult<Void>> redo(ErdOperationLog operationLog);

}
