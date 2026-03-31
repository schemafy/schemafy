package com.schemafy.core.erd.operation.domain;

public record CommittedErdOperation(
    String opId,
    String clientOperationId,
    long committedRevision,
    ErdOperationDerivationKind derivationKind) {

  public static CommittedErdOperation from(ErdOperationLog operationLog) {
    return new CommittedErdOperation(
        operationLog.opId(),
        operationLog.clientOperationId(),
        operationLog.committedRevision(),
        operationLog.derivationKind());
  }

}
