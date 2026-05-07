package com.schemafy.core.erd.operation.application.service;

import com.schemafy.core.erd.operation.domain.ErdOperationLog;

public record ResolvedUndoRedoEligibility(
    UndoRedoAction action,
    ErdOperationLog targetOperation,
    ErdOperationLog targetRootOriginalOperation,
    ErdOperationLog currentChainTipOperation,
    ErdOperationLog currentUndoCandidateOperation,
    ErdOperationLog currentRedoCandidateOperation,
    ErdOperationLog schemaHeadOperation) {

  public long schemaCurrentRevision() {
    return schemaHeadOperation.committedRevision();
  }

  public ErdOperationLog executionBaseOperation() {
    return action == UndoRedoAction.UNDO
        ? currentChainTipOperation
        : targetRootOriginalOperation;
  }

}
