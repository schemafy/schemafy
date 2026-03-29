package com.schemafy.api.erd.controller;

import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

final class ErdOperationFixtures {

  static final String OP_ID = "op-1";
  static final String CLIENT_OPERATION_ID = "client-op-1";
  static final long COMMITTED_REVISION = 42L;

  private ErdOperationFixtures() {
  }

  static CommittedErdOperation committedOperation() {
    return new CommittedErdOperation(
        OP_ID,
        CLIENT_OPERATION_ID,
        COMMITTED_REVISION,
        ErdOperationDerivationKind.ORIGINAL);
  }

}
